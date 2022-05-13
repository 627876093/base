package com.liglig.base.route

/**
 * created by liglig on 2021/4/9 0009
 * Description:
 */
object RoutePath {

    object Login{
        private const val LOGIN="/login"

        const val LOGIN_ACTIVITY="${LOGIN}/loginActivity"

        const val REGISTER_ACTIVITY="${LOGIN}/registerActivity"
    }

    object Main{
        private const val MAIN="/main"

        const val MAIN_ACTIVITY="${MAIN}/mainActivity"

        const val MAIN_SERVICE="${MAIN}/mainService"
    }
}