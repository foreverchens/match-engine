package icu.match.service.match.snapshot;/**
 *
 * @author 中本君
 * @date 2025/9/13
 */

import org.springframework.stereotype.Component;

import icu.match.service.match.MatchEngine;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author 中本君
 * @date 2025/9/13 
 */
@Slf4j
@Component
public class SnapshotManage {

	@Resource
	private MatchEngine matchingEngine;


	@PostConstruct
	public void init() {
		matchingEngine.depth();
	}
}
