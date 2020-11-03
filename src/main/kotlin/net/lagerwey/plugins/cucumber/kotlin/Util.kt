package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.application.ApplicationManager

fun <T> inReadAction(body: () -> T): T {
    return ApplicationManager.getApplication().run {
        if (isReadAccessAllowed) {
            body()
        } else runReadAction<T>(body)
    }
}
