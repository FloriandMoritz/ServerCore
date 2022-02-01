package net.eltown.servercore.components.data.bank;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BankLog {

    private final String logId;
    private final String title;
    private final String details;
    private final String date;

}
