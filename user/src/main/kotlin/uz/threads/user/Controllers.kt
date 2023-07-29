package uz.threads.user

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@ControllerAdvice
class ExceptionHandlers(
    private val errorMessageSource: ResourceBundleMessageSource
) {
    @ExceptionHandler(UserServiceException::class)
    fun handleException(exception: UserServiceException): ResponseEntity<*> {
        return when (exception) {
            is UserNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is CategoryNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is CategoryAlreadyExistsException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is EmailOrPhoneNotNullException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is UsernameAlreadyExistsException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is GenderNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<String> {
        val result = ex.bindingResult
        val errors = result.fieldErrors
        var message = ""
        for ((index, error) in errors.withIndex()) {
            message += "" + (index + 1) + ". ${error.defaultMessage}\n"
        }

        return ResponseEntity.badRequest().body(message)
    }
}

@RestController
@RequestMapping("category")
class CategoryController(
    private val service: CategoryService
) {
    @PostMapping("{name}")
    fun create(@PathVariable name: String) = service.create(name)

    @PutMapping
    fun update(@Valid @RequestBody dto: CategoryDto) = service.update(dto)

    @GetMapping
    fun get(pageable: Pageable) = service.get(pageable)

    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
class UserController(
    private val service: UserService
) {
    @PostMapping
    fun create(@Valid @RequestBody dto: UserCreateDto) = service.create(dto)

    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody @Valid dto: UserUpdateDto) = service.update(id, dto)

    @GetMapping
    fun get(pageable: Pageable) = service.get(pageable)

    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("internal")
class UserInternalController(
    private val service: UserService
) {
    @GetMapping("exists/{id}")
    fun existsById(@PathVariable id: Long) = service.existsById(id)
}