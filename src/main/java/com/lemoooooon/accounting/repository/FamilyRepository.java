package com.lemoooooon.accounting.repository;

import com.lemoooooon.accounting.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    // 透過邀請碼找家庭
    Optional<Family> findByInviteCode(String inviteCode);
    
    // 檢查邀請碼是否已存在 (生成時防撞用)
    boolean existsByInviteCode(String inviteCode);
}
