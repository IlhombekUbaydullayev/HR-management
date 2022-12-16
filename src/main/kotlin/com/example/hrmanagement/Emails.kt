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

    fun sendEmailTask(
        name: String,
        description: String,
        lifetime: String,
        responsible: String,
        user_id: String,
        random2: Long
    ):Boolean {
        try {
            val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, true)
            mimeMessageHelper.setFrom(responsible)
            mimeMessageHelper.setTo(user_id)
            mimeMessageHelper.setSubject("New Task")
            mimeMessageHelper.setText("Task : $name\nDescription : $description\nLifeTime : ${lifetime}\nresponsible : $responsible\nrandom2 : $random2")
            mimeMessageHelper.setText("<a href='http://localhost:8090/api/v1/task/get/api?name=$name&description=$description&lifetime=$lifetime&responsible=$responsible&user_id=$user_id&random2=$random2'>Enter your task</a>")
            javaMailSender.send(mimeMessage)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun sendTask(sendingEmail: String, name: String, remoteUser: String): Boolean {
        try {
            val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, true)
            mimeMessageHelper.setFrom(remoteUser)
            mimeMessageHelper.setTo(sendingEmail)
            mimeMessageHelper.setSubject("Task Done Successfully")
            mimeMessageHelper.setText("<a href='http://localhost:8090/api/v1/auth/verifyEmail?sendingEmail=$sendingEmail&remoteUser=$remoteUser&name=$name'>Tasdiqlang</a>")
            javaMailSender.send(mimeMessage)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}