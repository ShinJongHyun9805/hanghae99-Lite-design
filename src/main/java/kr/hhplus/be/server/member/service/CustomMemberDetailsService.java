package kr.hhplus.be.server.member.service;

import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomMemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 로그인
     * Spring Security가 UserDetailsService 구현체의 loadUserByUserName() 호출
     * 반환된 UserDetails 객체를 이용해 암호 검증 수행
     *
     * @param memberId 회원 아이디
     * @return UseDetails
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {

        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(InvalidUserException::new);

        List<SimpleGrantedAuthority> authorities = member.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getDisplayName()))
                .toList();

        return new User(member.getMemberId(), member.getPassword(), authorities);
    }
}
