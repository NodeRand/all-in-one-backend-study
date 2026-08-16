package com.group.library_app.repository.user;

import com.group.library_app.dto.user.response.UserResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isUserNotExist(long id){
        String readSql = "SELECT * FROM user WHERE id = ?";
        /* 행별로 람다식 실행 -> 최종 형태는 배열형태(조회 성공 시,
         하나의 행만 나오고 람다식 한번만 실행 -> 0 반환 -> [0], 없다면 []) */
        return jdbcTemplate.query(readSql, (rs,rowNum)->0, id).isEmpty();
    }

    public void updateUserName(String name, long id){
        String sql = "UPDATE user SET name = ? WHERE id = ?";
        jdbcTemplate.update(sql, name, id);
    }

    public boolean isUserNotExist(String name){
        String readSql = "SELECT * FROM user WHERE name = ?";
        /* 행별로 람다식 실행 -> 최종 형태는 배열형태(조회 성공 시,
         하나의 행만 나오고 람다식 한번만 실행 -> 0 반환 -> [0], 없다면 []) */
        return jdbcTemplate.query(readSql, (rs,rowNum)->0, name).isEmpty();
    }

    public void deleteUsername(String name){
        String sql="DELETE FROM user WHERE name = ?";
        jdbcTemplate.update(sql, name);
    }

    public void saveUser(String name, Integer age){
        String sql = "INSERT INTO user (name, age) VALUES (?, ?)";
        jdbcTemplate.update(sql, name, age);
    }

    public List<UserResponse> getUsers(){
        // db 사용 전 코드:
//        List<UserResponse> responses = new ArrayList<>(); // 여긴 private 없는 이유 -> 외부 response 용이라서?
//        for (int i = 0; i < users.size(); i++) {
//            responses.add(new UserResponse(i + 1, users.get(i)));
//        }
//        return responses;
        String sql = "SELECT * FROM user";
        // db의 user를 UserResponse로 바꿔주는 역할
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            int age = rs.getInt("age");
            return new UserResponse(id,name,age);
        });
    }
}
