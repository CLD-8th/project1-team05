package com.team5.bookexchange.service;

import com.team5.bookexchange.entity.Member;
import com.team5.bookexchange.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void signup(String username, String password, String name) {

        if (memberRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        Member member = new Member();
        member.setUsername(username);
        member.setPassword(password);
        member.setName(name);

        memberRepository.save(member);
    }

    public Member login(String username, String password) {

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.")
                );

        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return member;
    }
}