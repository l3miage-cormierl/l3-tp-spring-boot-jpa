package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BooksMapper booksMapper;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BooksMapper booksMapper) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.booksMapper = booksMapper;
    }

    @GetMapping("/authors")
    public Collection<AuthorDTO> authors(@RequestParam(value = "q", required = false) String query) {
        Collection<Author> authors;
        if (query == null) {
            authors = authorService.list();
        } else {
            authors = authorService.searchByName(query);
        }
        return authors.stream()
                .map(authorMapper::entityToDTO)
                .toList();
    }

    @GetMapping("/authors/{id}")
    public AuthorDTO author(@PathVariable("id") Long id) {
        try {
            Author author = authorService.get(id);
            return authorMapper.entityToDTO(author);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO newAuthor(@RequestBody AuthorDTO author) {
        // Question: Doit-on vérifier que l'id donné existe déjà? Qu'on a bien donné un
        // id et un nom? Que le nom n'est pas déjà dans la liste?
        if (author.fullName().trim().equals("")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST); // BAD_REQUEST = Code erreur de retour 400
        }
        Author authorTmp = authorMapper.dtoToEntity(author);

        authorTmp = authorService.save(authorTmp);

        return authorMapper.entityToDTO(authorTmp);
    }

    @PutMapping("/authors/{id}")
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO author, @PathVariable("id") Long id) {
        // attention AuthorDTO.id() doit être égale à id, sinon la requête utilisateur
        // est mauvaise
        if (author.id() != id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Author authorTmp;
        try {
            authorTmp = this.authorService.get(id);

            if (authorTmp == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }

            authorTmp = authorService.update(authorMapper.dtoToEntity(author));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return authorMapper.entityToDTO(authorTmp);
    }

    /*
     * récupérer la liste des livres que l'auteur à écrit. Si la liste n'est pas
     * vide alors il faut supprimer les livres avant
     * bon apparement c'est déjà pris en compte
     * Bon faudra regardé au niveau des catch et des throws c'est pas ça
     */
    @DeleteMapping("/authors/{id}")
    public void deleteAuthor(@PathVariable("id") Long id) throws EntityNotFoundException {
        Author authorTmp;
        try {
            authorTmp = this.authorService.get(id);
            // vérifier que l'auteur n'est pas le seul sur le bouquin : le livre doit être
            // retiré d'abord : erreur 400
            // prendre ses bouquins, verifier les auteurs (s'il y en a plus qu'un)
            Set<Book> authorBooks = authorTmp.getBooks();

            if (authorBooks != null) {
                for (Book book : authorBooks) {
                    if (book.getAuthors().size() > 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                    }
                }
            }
            this.authorService.delete(authorTmp.getId());
            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (DeleteAuthorException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

    /*
     * Requête pour récupérer la liste des livres associés à l'id de l'autheur donné
     * en paramètre
     * Dans le contrat c'est écrit "possibly filtered by name
     */
    @GetMapping("/authors/{id}/books")
    public Collection<BookDTO> books(@RequestParam(value = "q", required = false) String query,
            @PathVariable("id") Long authorId) throws EntityNotFoundException {
        Author author;
        try {
            author = authorService.get(authorId);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Collection<Book> authorBooks = author.getBooks();

        return authorBooks.stream()
                .map(booksMapper::entityToDTO)
                .toList();
    }
}
