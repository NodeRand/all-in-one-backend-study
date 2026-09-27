package com.group.library_app.service.book;

import com.group.library_app.domain.book.Book;
import com.group.library_app.domain.book.BookRepository;
import com.group.library_app.domain.user.User;
import com.group.library_app.domain.user.UserRepository;
import com.group.library_app.domain.user.loanhistory.UserLoanHistory;
import com.group.library_app.domain.user.loanhistory.UserLoanHistoryRepository;
import com.group.library_app.dto.book.request.BookCreateRequest;
import com.group.library_app.dto.book.request.BookLoanRequest;
import com.group.library_app.dto.book.request.BookReturnRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BookService {
    // private final BookMemoryRepository bookRepository = new BookMemoryRepository();
    // private final BookRepository bookRepository = new BookMemoryRepository();
    // 인터페이스를 쓰면 그냥 우변만 바꾸면 됨
    private final BookRepository bookRepository;
    private final UserLoanHistoryRepository userLoanHistoryRepository;
    private final UserRepository userRepository;

    public BookService(BookRepository bookRepository,
                       UserLoanHistoryRepository userLoanHistoryRepository,
                       UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userLoanHistoryRepository = userLoanHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveBook(BookCreateRequest request){
        bookRepository.save(new Book(request.getName()));
    }

    @Transactional
    public void loanBook(BookLoanRequest request){
        //1. 책 정보를 가져온다.
        // Optional 열기 · IllegalArgumentException::new(메서드 레퍼런스) 문법
        // → docs/optional-and-method-reference.md
        Book book = bookRepository.findByName(request.getBookName()).orElseThrow(IllegalArgumentException::new);

        //2. 대출기록 정보를 확인해서 대출 중인지 확인합니다.
        //3. 만약에 확인했는데 대출 중이라면 예외를 발생시킵니다.
        if(userLoanHistoryRepository.existsByBookNameAndIsReturn(book.getName(), false)){
            throw new IllegalArgumentException("이미 대출 중인 책입니다.");
        }

        //4. 유저 정보를 가져온다.
        User user = userRepository.findByName(request.getUserName()).orElseThrow(IllegalArgumentException::new);

        //5. 유저 정보와 책 정보를 기반으로 UserLoanHistory를 저장
        userLoanHistoryRepository.save(new UserLoanHistory(user.getId(), book.getName()));
    }

    @Transactional
    public void returnBook(BookReturnRequest request){
        // name 받은걸로 db에서 유저 찾기
        User user = userRepository.findByName(request.getUserName()).orElseThrow(IllegalArgumentException::new);

        // Optional<T> 열기 · orElseThrow가 좌변 타입을 T로 바꾸는 이유
        // → docs/optional-and-method-reference.md
        UserLoanHistory history = userLoanHistoryRepository.findByUserIdAndBookName(user.getId(), request.getBookName()).orElseThrow(IllegalArgumentException::new);
        history.doReturn();
        // userLoanHistoryRepository.save(history);가 필요 없는 이유?
        // => 이미 Transactional을 통해 user와 history는 영속성 컨텍스트로서 해당 엔티티객체와 디비객체가 연동, 자동 감지 업데이트가 진행됨
    }
}

