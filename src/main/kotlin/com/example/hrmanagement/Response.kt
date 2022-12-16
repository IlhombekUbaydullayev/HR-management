package com.example.hrmanagement


data class BaseMessage(val code: Int, val message: String) {

    companion object {

        val OK = BaseMessage(200, "Success")
        val DELETE=BaseMessage(200,"Delete")
        val Not_Found= BaseMessage(404,"Not Found")
    }
}



