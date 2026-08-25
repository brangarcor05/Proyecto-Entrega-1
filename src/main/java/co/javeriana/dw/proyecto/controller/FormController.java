package co.javeriana.dw.proyecto.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FormController {

    @GetMapping("/")
    public String mostrarFormulario() {
        return "index";
    }

    @PostMapping("/procesar")
    public String procesarFormulario(
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam Integer edad,
            @RequestParam String mensaje,
            Model model) {
        
        // Guardamos los datos en el modelo
        model.addAttribute("nombre", nombre);
        model.addAttribute("email", email);
        model.addAttribute("edad", edad);
        model.addAttribute("mensaje", mensaje);
        
        return "resultado";
    }
}