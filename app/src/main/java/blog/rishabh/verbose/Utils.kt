package blog.rishabh.verbose

class Utils {
    companion object {
        fun median(list: ArrayList<Int>): Int = list.let {
            if (it.size % 2 == 0)
                (it[it.size / 2] + it[(it.size - 1) / 2]) / 2
            else
                it[it.size / 2]
        }
    }
}