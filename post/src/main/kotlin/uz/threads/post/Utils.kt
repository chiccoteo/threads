//package uz.threads.post
//
//import org.springframework.data.domain.PageRequest
//import org.springframework.data.domain.Pageable
//import org.springframework.data.domain.Sort
//import org.springframework.stereotype.Component
//
//@Component
//class CommandUtils {
//
//    private fun validatePageAndSize(page: Int, size: Int) {
//        if (page < 0) {
//            throw PageSizeInvalidException()
//        } else if (size <= 0) {
//            throw PageSizeInvalidException()
//        } else if (size > 10) {
//            throw PageSizeInvalidException()
//        }
//    }
//
//    fun simplePageable(page: Int, size: Int): Pageable {
//        validatePageAndSize(page, size)
//        return PageRequest.of(page, size, Sort.by("id"))
//    }
//}