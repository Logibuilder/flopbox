package univ.sr2.flopbox.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import univ.sr2.flopbox.dto.UserRequest;
import univ.sr2.flopbox.model.User;
import univ.sr2.flopbox.repository.UserRepository;


@Transactional
@Service
public class UserService {


    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;


    public UserRequest register(UserRequest userRequest) {

        if (userRepository.findByMail(userRequest.mail()).isPresent()) throw new RuntimeException("L'utilisateur " + userRequest.mail() + " existe déjà");

        try {
            User user = userRequest.toUser();
            user.setPassword(passwordEncoder.encode(userRequest.password()));
            userRepository.save(user);
            return userRequest;
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Erreur d'intégrité : l'email est peut-être déjà utilisé.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur technique lors de la sauvegarde : " + e.getMessage());
        }
    }
}
