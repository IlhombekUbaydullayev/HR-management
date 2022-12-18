package com.example.hrmanagement

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtFilter : OncePerRequestFilter() {
    @Autowired
    lateinit var jwtProvider: JwtProvider

    @Autowired
    lateinit var authService: AuthServiceImp

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var authorization = httpServletRequest.getHeader("Authorization")
        if (authorization != null && authorization.startsWith("Bearer")) {
            authorization = authorization.substring(7)
            val email = jwtProvider.getEmailFromToken(authorization)
            if (email != null) {
                val userDetails = authService.loadUserByUsername(email)
                val usernamePasswordAuthenticationToken =
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                SecurityContextHolder.getContext().authentication = usernamePasswordAuthenticationToken
            }
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse)
    }
}

@Component
class JwtProvider {
    fun generateToken(username: String?, systemRoleName: CompanyRoleName): String {
        val expireDate = Date(System.currentTimeMillis() + expireTime)
        return Jwts
            .builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(expireDate)
            .claim("roles", systemRoleName.name)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact()
    }

    fun getEmailFromToken(token: String?): String? {
        return try {
            Jwts
                .parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getSubject()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val expireTime = (1000 * 60 * 60 * 24).toLong()
        private const val secretKey = "maxfiysuzhechkimbilmasin"
    }
}

class SpringSecurityAuditAwareImpl : AuditorAware<Long> {
    override fun getCurrentAuditor(): Optional<Long> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (!(authentication == null || !authentication.isAuthenticated || "anonymousUser" == "" + authentication.principal)) {
            val uuid = (authentication.principal as User).id
            return Optional.of(uuid!!)
        }
        return Optional.empty()
    }
}