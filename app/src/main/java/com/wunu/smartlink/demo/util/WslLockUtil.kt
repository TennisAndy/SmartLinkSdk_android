package com.wunu.smartlink.demo.util

import android.content.Context
import com.wunu.smartlink.demo.R

object WslLockUtil {
    fun getLockType(ctx: Context, devName:String): String{
        val lockType:String
        when(devName.substring(4, 5)){
            "O","U" ->{
                lockType = ctx.getString(R.string.lock_type_other)
            }
            "N","M","D" ->{
                lockType = ctx.getString(R.string.lock_type_nb)
            }
            "F" ->{
                lockType = ctx.getString(R.string.lock_type_f)
            }
            "C" ->{
                lockType = ctx.getString(R.string.lock_type_c)
            }
            else ->{
                lockType = ctx.getString(R.string.lock_type_other)
            }
        }
        return lockType
    }
    
    fun getLockModel(name: String?): Int{
        if (name == null) return 0
        if (name.length == 9) {
            return 1
        } else {
            if (name.indexOf("WSL_A") == 0) {
                return 1 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_H") == 0) {
                return 10 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_B") == 0) {
                return 21 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_N") == 0) {
                return 30 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_M") == 0) {
                return 40 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_U") == 0) {
                return 50 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_J") == 0) {
                return 60 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_F") == 0) {
                return 70 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_C") == 0) {
                return 80 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_O") == 0) {
                return 90 + name.substring(5, 6).toInt()
            } else if (name.indexOf("WSL_D") == 0) {
                return 100 + name.substring(5, 6).toInt()
            } else {
                return 0
            }
        }
    }

    fun randomPrime6():Int{
        var random = Math.floor( Math.random() * 900000 + 100000).toInt()
        if (isPrime(random)) return random

        //取rondom最近的一个素数
        var index = 0
        var prime = 100003
        while (true) {
            index++

            if (isPrime(random + index)) {
                prime = random + index
                break
            }

            if (isPrime(random - index)) {
                prime = random - index
                break
            }
        }
        return prime
    }

    fun isPrime(num: Int): Boolean {
        if (num == 2) return true//2特殊处理
        if (num < 2 || num % 2 == 0) return false//识别小于2的数和偶数
        var i = 3
        while (i <= Math.sqrt(num.toDouble())) {
            if (num % i == 0) {//识别被奇数整除
                return false
            }
            i += 2
        }
        return true
    }
}