package net.eltown.servercore.components.data.bank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BankAccount {

    private final String account;
    private String displayName;
    private final String owner;
    private String password;
    private double balance;
    private List<BankLog> bankLogs;

}
