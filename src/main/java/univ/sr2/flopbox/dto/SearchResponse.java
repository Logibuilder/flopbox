package univ.sr2.flopbox.dto;

import univ.sr2.flopbox.model.Server;

import java.util.Map;

public record SearchResponse(
        FtpItem ftpItem,
        ServerRequest serverRequest
) {
}
