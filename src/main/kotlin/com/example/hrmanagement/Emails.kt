package com.example.hrmanagement

import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import javax.mail.internet.MimeMessage

@Configuration
class Emails(
    val javaMailSender: JavaMailSender
){
    fun sendEmail(sendingEmail: String, emailCode: String,company : String): Boolean {
        try {
            val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, true)
            mimeMessageHelper.setFrom("ubaydullaevilhombek681@gmail.com")
            mimeMessageHelper.setTo(sendingEmail)
            mimeMessageHelper.setSubject("Accountni Tasdiqlash")
            mimeMessageHelper.setText("<a href='http://localhost:8090/api/v1/auth/verifyEmail?emailCode=$emailCode&email=$sendingEmail&company=$company'>Tasdiqlang</a>")
            javaMailSender.send(mimeMessage)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}