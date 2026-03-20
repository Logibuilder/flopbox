package univ.sr2.flopbox.dto;

public record RenameRequest(
        String oldName,
        String newName
) {
}
