package icu.match.core;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 价位内订单队列：双向链表 + orderId 索引。
 *
 * @author 中本君
 * @date 2025/8/12
 */
public class OrderQueue {

	/**
	 *  orderId -> 节点映射（O(1) 定位/删除/修改）
	 */
	private final Map<Long, OrderNode> byId = new HashMap<>(64);

	/**
	 * 队列元素个数
	 */
	@Getter
	private int size;

	/**
	 * 队列聚合数量（所有节点 qty 之和）。
	 */
	@Getter
	private long totalQty;

	private OrderNode head;

	private OrderNode tail;

	/**
	 * 获取队头（不移除）。
	 *
	 * @return 队头节点，若为空则返回 null
	 */
	public OrderNode peek() {return head;}

	/**
	 * 队列是否为空。
	 */
	public boolean isEmpty() {return size == 0;}

	/**
	 * 根据订单 ID 获取节点。
	 *
	 * @param orderId 订单 ID
	 * @return 节点或 null
	 */
	public OrderNode get(long orderId) {return byId.get(orderId);}

	/**
	 * 尾插入队（FIFO）。
	 * <p>该方法只处理链表与索引，不负责节点回收。</p>
	 *
	 * @param node 由上层分配且当前未链接的节点
	 * @throws IllegalArgumentException 节点为 null
	 * @throws IllegalStateException    节点已链接或队内存在相同 orderId
	 */
	public void push(OrderNode node) {
		if (node == null) {
			throw new IllegalArgumentException("node must not be null");
		}
		if (node.prev != null || node.next != null) {
			throw new IllegalStateException("node already linked: orderId=" + node.orderId);
		}
		if (byId.containsKey(node.orderId)) {
			throw new IllegalStateException("duplicate orderId in queue: " + node.orderId);
		}

		linkAtTail(node);
		byId.put(node.orderId, node);
		size++;
		totalQty += node.qty;
	}

	/**
	 * 将节点挂到链表尾部。
	 */
	private void linkAtTail(OrderNode n) {
		if (tail == null) {
			head = tail = n;
		}
		else {
			n.prev = tail;
			tail.next = n;
			tail = n;
		}
	}

	/**
	 * 按 orderId 删除（仅 unlink，不负责回收）。
	 *
	 * @param orderId 订单 ID
	 * @return 被移除的节点；若不存在返回 null
	 */
	public OrderNode remove(long orderId) {
		OrderNode n = byId.remove(orderId);
		if (n == null) {
			return null;
		}
		unlink(n);
		size--;
		totalQty -= n.qty;
		return n;
	}

	/**
	 * 从链表中摘除指定节点（不负责更新索引或回收）。
	 */
	private void unlink(OrderNode n) {
		OrderNode p = n.prev, nx = n.next;
		if (p == null) {
			head = nx;
		}
		else {
			p.next = nx;
		}
		if (nx == null) {
			tail = p;
		}
		else {
			nx.prev = p;
		}
		n.prev = n.next = null;
	}

	/**
	 * 按 orderId 修改数量，并同步更新聚合数量。
	 *
	 * @param orderId 订单 ID
	 * @param newQty  新的数量值（不允许为 0；合法性由上层保证）
	 * @return true 表示修改成功；false 表示未找到
	 */
	public boolean patchQty(long orderId, long newQty) {
		if (newQty <= 0) {
			throw new IllegalArgumentException("newQty must be greater than 0");
		}
		OrderNode node = byId.get(orderId);
		if (node == null) {
			return false;
		}
		long delta = newQty - node.qty;
		node.qty = newQty;
		totalQty += delta;
		return true;
	}

	// —— 内部链表操作 ——

	/**
	 * 清空队列（仅 unlink，不负责回收）。
	 * <p>上层可随后遍历已摘除链表进行统一回收。</p>
	 *
	 * @return 清空前的头结点（可用于后续遍历）；若本来为空返回 null
	 */
	public OrderNode clear() {
		OrderNode first = head;
		head = tail = null;
		size = 0;
		totalQty = 0L;
		byId.clear();
		return first;
	}

	/**
	 * 生成队列快照（检测环并提前终止）。
	 */
	public String dump() {
		StringBuilder sb = new StringBuilder(128 + size * 48);
		sb.append("OrderQueue{size=")
		  .append(size)
		  .append(", totalQty=")
		  .append(totalQty)
		  .append("}\n [HEAD] ");
		int visited = 0;
		OrderNode cur = head;
		while (cur != null) {
			sb.append("(id=")
			  .append(cur.orderId)
			  .append(", qty=")
			  .append(cur.qty)
			  .append(", t=")
			  .append(cur.time)
			  .append(", pooled=")
			  .append(cur.pooled)
			  .append(")");
			cur = cur.next;
			visited++;
			if (cur != null) {
				sb.append(" -> ");
			}
			if (visited > size && size > 0) {
				sb.append(" ...[cycle detected]");
				break;
			}
		}
		sb.append(" [TAIL]");
		return sb.toString();
	}
}
