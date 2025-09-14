package icu.match.service.match.snapshot;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import org.springframework.stereotype.Component;

import icu.match.core.snapshot.CowPool;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
@Slf4j
@Component
public class SnapshotManage extends CowPool {


	@PostConstruct
	public void init() {

	}


}
