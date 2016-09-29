package no.difi.meldingsutveksling.serviceregistry.mock;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AddMockUserController {

    @RequestMapping("/addmock")
    public String addMockUser() {
        return "addmock";
    }
}
