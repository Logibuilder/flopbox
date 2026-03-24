package univ.sr2.flopbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import univ.sr2.flopbox.model.User;

public record UserRequest(
        @NotNull
        String mail,
        String nom,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password
) {
    public User toUser() {

        return new User(null, this.mail, this.nom, this.password);
    }

    public static UserRequest toUserRequest(User user) {
        return new UserRequest(user.getMail(), user.getName(), null);
    }
}
