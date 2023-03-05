package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BooksMapper booksMapper;
    private final BookService bookService;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BooksMapper booksMapper, BookService bookService) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.booksMapper = booksMapper;
        this.bookService = bookService;
    }

    /*Get all authors*/
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

    /*Get an author*/
    @GetMapping("/authors/{id}")
    public AuthorDTO author(@PathVariable("id") Long id) {
        Author author = null; 
        try {
            author = authorService.get(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return authorMapper.entityToDTO(author);
    }

    /*Create an author*/
    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO newAuthor(@RequestBody AuthorDTO author) {
        if (author.fullName().trim().equals("")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Author authorTmp = authorMapper.dtoToEntity(author);
        authorTmp = authorService.save(authorTmp);
        return authorMapper.entityToDTO(authorTmp);
    }

    /*Update an author*/
    @PutMapping("/authors/{id}")
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO author, @PathVariable("id") Long id) {
        Author authorTmp = null;
        if (author.id() != id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            authorTmp = this.authorService.get(id);
            authorTmp = authorService.update(authorMapper.dtoToEntity(author));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return authorMapper.entityToDTO(authorTmp);
    }

    /*Delete an author*/
    @DeleteMapping("/authors/{id}")
    public void deleteAuthor(@PathVariable("id") Long id) {
        try {
            Author authorTmp = this.authorService.get(id);
            this.authorService.delete(authorTmp.getId());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (DeleteAuthorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        throw new ResponseStatusException(HttpStatus.NO_CONTENT);

    }

    /*Find all books for a given author, possibly filtered by name*/
    @GetMapping("/authors/{id}/books")
    public Collection<BookDTO> books(@PathVariable("id") Long authorId, @RequestParam(value = "q", required = false) String query) {
        Collection<Book> books; 
        if(query == null){
            try {
                books = bookService.getByAuthor(authorId);
            } catch (EntityNotFoundException e) {
                throw new ResponseStatusException((HttpStatus.NOT_FOUND));
            }
        } else {
            try {
                books = bookService.findByAuthor(authorId, query);
            } catch (EntityNotFoundException e) {
                throw new ResponseStatusException((HttpStatus.NOT_FOUND));
            }
        }
        return booksMapper.entityToDTO(books);
    }
}
