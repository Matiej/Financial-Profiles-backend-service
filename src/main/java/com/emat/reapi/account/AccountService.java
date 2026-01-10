package com.emat.reapi.account;

import com.emat.reapi.account.domain.Account;
import com.emat.reapi.account.domain.UpdateProfileCommand;
import reactor.core.publisher.Mono;

public interface AccountService {

    Mono<Account> getAccount(String userId);

    Mono<Account> updateProfile(String userId, UpdateProfileCommand command);

    Mono<Void> requestPasswordReset(String userId);

    Mono<Void> deleteAccount(String userId);
}
