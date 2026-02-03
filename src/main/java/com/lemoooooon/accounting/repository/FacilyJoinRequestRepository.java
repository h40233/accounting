package com.lemoooooon.accounting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lemoooooon.accounting.model.FacilyJoinRequest;
import com.lemoooooon.accounting.model.FacilyJoinRequest.Status;

public interface FacilyJoinRequestRepository extends JpaRepository<FacilyJoinRequest, Long> {

    // Host 查看自己家庭的待審核清單
    List<FacilyJoinRequest> findByFamilyHostGoogleIdAndStatus(String hostGoogleId, Status status);

    // Host 只能操作自己家庭的申請
    Optional<FacilyJoinRequest> findByIdAndFamilyHostGoogleId(Long id, String hostGoogleId);

    // 檢查是否已經對同一個家庭送出過待審核申請
    boolean existsByApplicantGoogleIdAndFamilyIdAndStatus(String applicantGoogleId,
                                                          Long familyId,
                                                          Status status);
}

