package de.deltatree.tools.rag.controller;

import de.deltatree.tools.rag.repository.DocumentEmbeddingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentEmbeddingRepository repository;

    public DocumentController(DocumentEmbeddingRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String listDocuments(Model model) {
        model.addAttribute("documents", repository.findAllOrderByCreatedAtDesc());
        return "documents";
    }

    @PostMapping("/delete/{id}")
    public String deleteDocument(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/documents";
    }
}
