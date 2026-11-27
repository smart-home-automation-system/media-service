package cloud.cholewa.media.amplifier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

//The amplifier (HAS-515): POST /amplifier/unlock sends the file that unhangs it after a loss of the grid - a write to
//the house by a member of the household, from the dashboard. 202 once the amplifier took the file (it should work
//again - there is no state to read back); 400 UNKNOWN_MEMBER, 409 UNLOCK_IN_PROGRESS, 502
//AMPLIFIER_REFUSED, 503 AMPLIFIER_UNAVAILABLE / REGISTRY_UNAVAILABLE, 504 AMPLIFIER_TIMEOUT (it may have been taken)
@RestController
@RequestMapping("/amplifier")
@RequiredArgsConstructor
public class AmplifierController {

    private final AmplifierService amplifierService;

    @PostMapping("/unlock")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> unlock(
        @RequestParam @Pattern(regexp = "dashboard", message = "source must be dashboard") final String source,
        @RequestParam @NotBlank @Size(max = 50) final String member
    ) {
        return amplifierService.unlock(source, member);
    }
}
