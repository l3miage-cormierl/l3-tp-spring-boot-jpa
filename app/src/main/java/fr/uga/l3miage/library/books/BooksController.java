package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.data.domain.Book.Language;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.Collection;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class BooksController {

    private final BookService bookService;
    private final BooksMapper booksMapper;

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper) {
        this.bookService = bookService;
        this.booksMapper = booksMapper;
    }

    /*
     * Requête qui permet de récupérer tous les livres si l'argument query et
     * absent, ou les livres ayant le titre donné
     * par le paramètre query
     */
    @GetMapping("/books") // @GetMapping("/books/v1") à la base il y avait v1 je sais pas pourquoi
    public Collection<BookDTO> books(@RequestParam(value = "q", required = false) String query) {
        Collection<Book> books;
        if (query == null) {
            books = bookService.list();
        } else {
            books = bookService.findByTitle(query);
        }
        return books.stream()
                .map(booksMapper::entityToDTO)
                .toList();
    }

    // Requête pour récupérer un livre par rapport à son id
    @GetMapping("/books/{id}")
    public BookDTO book(@PathVariable("id") Long id) {
        Book book;
        try {
            book = bookService.get(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(book);
    }

    private boolean isValid(Book book) {
        boolean isValid = true;
        int year = Year.now().getValue();

        if (book.getTitle() == null || book.getYear() > year || book.getIsbn() < 1000000000L
                || book.getIsbn() >= 9999999999999L) {
            isValid = false;
        }

        int cpt = 0;
        for (Language language : Language.values()) {
            if (language.equals(book.getLanguage())) {
                cpt++;
            }
        }
        if (cpt == 0) {
            isValid = false;
        }

        return isValid;
    }

    /*
     * Requête pour ajouter un nouveau livre (C'est à nous de vérifier que le livre
     * existe déjà, que l'auteur est dans
     * la BD ou c'est le service qui gère ça?
     * Je crois que ce create est pas dans la spec? Ou bien c'est celui ayant le
     * chemin /api/authors/{id}/books
     */
    @PostMapping("/authors/{id}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("id") Long authorId, @RequestBody BookDTO book) {
        Book newBook;
        try {
            newBook = booksMapper.dtoToEntity(book);

            if (isValid(newBook) == false) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            newBook = bookService.save(authorId, newBook);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(newBook);
    }

    @PutMapping("/books/{id}")
    public BookDTO updateBook(@PathVariable("id") Long authorId, @RequestBody BookDTO book) {
        // attention BookDTO.id() doit être égale à id, sinon la requête utilisateur est
        // mauvaise
        Book bookTmp = booksMapper.dtoToEntity(book);

        if (book.id() != authorId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (bookTmp == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            bookTmp = bookService.update(bookTmp);
            bookTmp = bookService.addAuthor(bookTmp.getId(), authorId);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return booksMapper.entityToDTO(bookTmp);
    }

    @PutMapping("/books/{id}/authors")
    public BookDTO addAuthorToSecondBook(@PathVariable("id") Long bookId, @RequestBody AuthorDTO author)
            throws EntityNotFoundException {
        return booksMapper.entityToDTO(bookService.addAuthor(bookId, author.id()));
    }

    @DeleteMapping("/books/{id}")
    public void deleteBook(@PathVariable("id") Long id) {
        try {
            Book bookTmp = bookService.get(id);
            if (bookTmp == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (bookTmp.getAuthors().size() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            bookService.delete(bookTmp.getId());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

}
