package com.victory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * EnableScheduling: 연습읽기 성취도 일별 스냅샷(PracticeAchievementSnapshotScheduler)의
 * @Scheduled 자정 실행을 위해 추가. 다른 기존 기능은 스케줄러를 쓰지 않는다.
 */
@SpringBootApplication
@EnableScheduling
public class VictoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(VictoryApplication.class, args);
	}

}
