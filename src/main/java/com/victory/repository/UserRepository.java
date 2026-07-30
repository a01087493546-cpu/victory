package com.victory.repository;

import com.victory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    /*
     * 개별읽기 책 등록 시 "학생당 진행 중 책 1권" 규칙을 동시 요청에서도
     * 지키기 위해 학생 행 자체를 잠근다. 진행 중 기록이 아직 없는 상태에서는
     * 잠글 reading_records 행이 없으므로(phantom read 문제), 대신 그
     * 학생의 users 행을 잠가 같은 학생의 등록 요청들이 직렬화되게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
