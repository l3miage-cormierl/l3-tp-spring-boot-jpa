package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    
    /*Find all books, possibly filtered by name*/
    @GetMapping("/books")
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

    /*Get a book*/
    @GetMapping("/books/{id}")
    public BookDTO book(@PathVariable("id") Long id) {
        Book book = null;
        try {
            book = bookService.get(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(book);
    }

    /*Create a new book for a given author*/
    @PostMapping("/authors/{id}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("id") Long authorId, @RequestBody BookDTO book) {
        Book newBook = null;
        if (!isAValidBook(book)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            newBook = booksMapper.dtoToEntity(book);
            newBook = bookService.save(authorId, newBook);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(newBook);
    }

    private boolean isAValidBook(BookDTO book) {
        boolean isValid = true;
        if (book.title() == null) {
            isValid = false;
        } else if (book.year() < -9999 || book.year() > 9999) {
            isValid = false;
        } else if (book.isbn() < 1000000000L || book.isbn() > 9999999999999L) {
            isValid = false;
        } else if (book.language() != null) {
            if (!book.language().equals("french") && !book.language().equals("english")) {
                isValid = false;
            }
        }
        return isValid;
    }

    /*Update a book*/
    @PutMapping("/books/{id}")
    public BookDTO updateBook(@PathVariable("id") Long authorId, @RequestBody BookDTO book) {
        // attention BookDTO.id() doit être égale à id, sinon la requête utilisateur est
        // mauvaise
        if (book.id() != authorId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Book bookTmp = booksMapper.dtoToEntity(book);
        try {
            bookTmp = bookService.update(bookTmp);
            bookTmp = bookService.addAuthor(bookTmp.getId(), authorId);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(bookTmp);
    }

    /*Delete a book*/
    @DeleteMapping("/books/{id}")
    public void deleteBook(@PathVariable("id") Long id) {
        try {
            Book bookTmp = bookService.get(id);
            bookService.delete(bookTmp.getId());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        throw new ResponseStatusException(HttpStatus.NO_CONTENT);
    }

    /*Add an author to a book*/
    @PutMapping("/books/{id}/authors")
    public BookDTO addAuthor(@PathVariable("id") Long bookId, @RequestBody AuthorDTO author) {
        Book book = null;
        try {
            book = bookService.addAuthor(bookId, author.id());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(book);
    }
}
