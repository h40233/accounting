package com.lemoooooon.accounting.repository;

import com.lemoooooon.accounting.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // 找出某個會員的所有帳戶
    List<Account> findByMemberGoogleId(String googleId);
}