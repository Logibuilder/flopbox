package univ.sr2.flopbox.controller;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/server")
public class ServerController {

    @GetMapping
    public String hello() {
        return "Hello";
    }

    //@GetMapping()
}
