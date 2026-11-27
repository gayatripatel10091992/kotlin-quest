package com.gayatripatel.kotlinquest.core

import com.gayatripatel.kotlinquest.core.ChoiceKind.CONCEPT
import com.gayatripatel.kotlinquest.core.ChoiceKind.FIX_THE_BUG
import com.gayatripatel.kotlinquest.core.ChoiceKind.PREDICT_OUTPUT
import com.gayatripatel.kotlinquest.core.Exercise.ArrangeTokens
import com.gayatripatel.kotlinquest.core.Exercise.Choice
import com.gayatripatel.kotlinquest.core.Exercise.FillBlank

/**
 * The built-in course: four units, each tied to the Kotlin feature real Android teams cite for adopting it.
 * Case-study blurbs are short summaries of public reporting, not statements from the companies.
 */
object CourseContent {

    private val nullSafety = CourseUnit(
        id = "null-safety",
        title = "Null safety",
        subtitle = "The feature most teams switched for",
        lessons = listOf(
            Lesson(
                "ns-1", "Nullable types",
                listOf(
                    Choice(
                        "ns-1-1", CONCEPT,
                        "Which declaration lets `name` hold null?",
                        listOf("val name: String = null", "val name: String? = null", "val name = null as String", "val name: String! = null"),
                        correctIndex = 1,
                        explanation = "A trailing `?` makes a type nullable. `String!` is a platform type the compiler shows for Java values; you can't write it yourself.",
                    ),
                    Choice(
                        "ns-1-2", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("0", "null", "NullPointerException", "Compile error"),
                        correctIndex = 1,
                        explanation = "The safe call `?.` returns null instead of calling `length` on a null receiver.",
                        code = "val city: String? = null\nprintln(city?.length)",
                    ),
                    FillBlank(
                        "ns-1-3",
                        "`name` is a `String?`. Fill the gap so this compiles and can never throw.",
                        code = "val len: Int = name___length ?: 0",
                        options = listOf("?.", ".", "!!.", "::"),
                        correctIndex = 0,
                        explanation = "`.` doesn't compile on a nullable receiver, and `!!.` compiles but throws on null. `?.` plus the Elvis `?: 0` is safe.",
                    ),
                    ArrangeTokens(
                        "ns-1-4",
                        "Store the length of `name`, or 0 if it's null.",
                        solution = listOf("val", "len", "=", "name", "?.", "length", "?:", "0"),
                        distractors = listOf("!!", "null"),
                        explanation = "Safe call, then Elvis for the fallback: `name?.length ?: 0`.",
                    ),
                    Choice(
                        "ns-1-5", FIX_THE_BUG,
                        "`greet(null)` crashes. Which fix is safest?",
                        listOf(
                            "Use `user?.name ?: \"guest\"`",
                            "Wrap the call in try/catch(Exception)",
                            "Write `user!!.name!!`",
                            "Change the parameter type to Any",
                        ),
                        correctIndex = 0,
                        explanation = "`!!` turns a null into a NullPointerException. Handle the null case where it happens instead of catching the crash later.",
                        code = "fun greet(user: User?) {\n    println(\"Hi \" + user!!.name)\n}\n\ngreet(null)",
                        highlightLine = 2,
                    ),
                ),
            ),
            Lesson(
                "ns-2", "Smart casts & let",
                listOf(
                    Choice(
                        "ns-2-1", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("6", "Kotlin", "Compile error", "null"),
                        correctIndex = 0,
                        explanation = "After the `is String` check the compiler smart-casts `input`, so `length` is available without a cast.",
                        code = "val input: Any = \"Kotlin\"\nif (input is String) {\n    println(input.length)\n}",
                    ),
                    Choice(
                        "ns-2-2", CONCEPT,
                        "What does `value?.let { save(it) }` do when `value` is null?",
                        listOf("Calls save(null)", "Skips the block and returns null", "Throws NullPointerException", "Doesn't compile"),
                        correctIndex = 1,
                        explanation = "The safe call short-circuits: `let` never runs, and the whole expression is null.",
                    ),
                    FillBlank(
                        "ns-2-3",
                        "Show \"Untitled\" when the title is null.",
                        code = "val title = article.title ___ \"Untitled\"",
                        options = listOf("?:", "?.", "!!", "||"),
                        correctIndex = 0,
                        explanation = "The Elvis operator `?:` returns its right side when the left side is null.",
                    ),
                    Choice(
                        "ns-2-4", FIX_THE_BUG,
                        "`refresh()` can run before the view is created. What prevents the crash?",
                        listOf(
                            "Check `::adapter.isInitialized` first",
                            "Declare it `lateinit val`",
                            "Call `adapter!!.notifyDataSetChanged()`",
                            "Mark the class `open`",
                        ),
                        correctIndex = 0,
                        explanation = "Reading a `lateinit` property before it's set throws UninitializedPropertyAccessException. `::adapter.isInitialized` checks first. `lateinit val` isn't allowed.",
                        code = "class FeedFragment : Fragment() {\n    lateinit var adapter: FeedAdapter\n\n    fun refresh() = adapter.notifyDataSetChanged()\n}",
                        highlightLine = 4,
                    ),
                ),
            ),
        ),
        caseStudies = listOf(
            CaseStudy("Swiggy", "Food delivery", "After moving new feature work to Kotlin (about 74% of the app), crashes reportedly fell by half."),
            CaseStudy("Zomato", "Food delivery", "Adopted Kotlin for shorter, safer code with fewer lines and a fail-fast approach to errors."),
        ),
    )

    private val dataAndSealed = CourseUnit(
        id = "data-sealed",
        title = "Data & sealed classes",
        subtitle = "Model state the compiler can check",
        lessons = listOf(
            Lesson(
                "ds-1", "Data classes",
                listOf(
                    Choice(
                        "ds-1-1", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("true", "false", "Compile error", "Point(x=1, y=2)"),
                        correctIndex = 0,
                        explanation = "Data classes generate `equals()` from the constructor properties, so two points with the same values are equal.",
                        code = "data class Point(val x: Int, val y: Int)\nprintln(Point(1, 2) == Point(1, 2))",
                    ),
                    Choice(
                        "ds-1-2", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("User(name=Asha, age=31)", "User(Asha, 31)", "User@1b6d3586", "31"),
                        correctIndex = 0,
                        explanation = "`copy()` makes a new instance with one property changed, and the generated `toString()` lists every property.",
                        code = "data class User(val name: String, val age: Int)\nval u = User(\"Asha\", 30)\nprintln(u.copy(age = 31))",
                    ),
                    ArrangeTokens(
                        "ds-1-3",
                        "Declare an immutable data class `Song` with a String `title`.",
                        solution = listOf("data", "class", "Song", "(", "val", "title", ":", "String", ")"),
                        distractors = listOf("var", "object"),
                        explanation = "`val` keeps the property read-only. Change it with `copy()` instead of mutating it.",
                    ),
                    Choice(
                        "ds-1-4", CONCEPT,
                        "What makes `val (name, age) = user` work?",
                        listOf("Generated componentN() functions", "Reflection", "The copy() function", "Conversion to a Pair"),
                        correctIndex = 0,
                        explanation = "Data classes generate `component1()`, `component2()` and so on, one per constructor property, and destructuring calls them.",
                    ),
                ),
            ),
            Lesson(
                "ds-2", "Sealed types & when",
                listOf(
                    Choice(
                        "ds-2-1", CONCEPT,
                        "Why does `when (state)` over this type need no `else` branch?",
                        listOf(
                            "The compiler knows every subtype, so it can prove the when is exhaustive",
                            "`when` never needs else",
                            "Data objects are always null",
                            "Sealed types are enums at runtime",
                        ),
                        correctIndex = 0,
                        explanation = "All subtypes of a sealed type are known at compile time. Add a new one and every `when` that misses it stops compiling.",
                        code = "sealed interface UiState {\n    data object Loading : UiState\n    data class Success(val items: List<String>) : UiState\n    data class Error(val message: String) : UiState\n}",
                    ),
                    Choice(
                        "ds-2-2", PREDICT_OUTPUT,
                        "With the same sealed `UiState`, what does this print?",
                        listOf("Oops: Offline", "Loading…", "0 items", "Compile error"),
                        correctIndex = 0,
                        explanation = "`is UiState.Error` smart-casts `state`, so `state.message` is available in that branch.",
                        code = "val state: UiState = UiState.Error(\"Offline\")\nval text = when (state) {\n    UiState.Loading -> \"Loading…\"\n    is UiState.Success -> \"\${state.items.size} items\"\n    is UiState.Error -> \"Oops: \${state.message}\"\n}\nprintln(text)",
                    ),
                    Choice(
                        "ds-2-3", FIX_THE_BUG,
                        "This doesn't compile. Why?",
                        listOf(
                            "The when isn't exhaustive: WALLET is missing",
                            "Enums can't be used in when",
                            "fee() must return Int?",
                            "Each branch needs braces",
                        ),
                        correctIndex = 0,
                        explanation = "A `when` used as an expression must cover every case. Add `Payment.WALLET -> …`, and the compiler will flag the next value you add too.",
                        code = "enum class Payment { CARD, UPI, WALLET }\n\nfun fee(p: Payment): Int = when (p) {\n    Payment.CARD -> 2\n    Payment.UPI -> 0\n}",
                        highlightLine = 3,
                    ),
                    FillBlank(
                        "ds-2-4",
                        "Give the singleton a readable toString() and equals().",
                        code = "___ object Loading : UiState",
                        options = listOf("data", "open", "lateinit", "inner"),
                        correctIndex = 0,
                        explanation = "A `data object` prints as `Loading` instead of `Loading@4e25154f`, which makes logs and tests readable.",
                    ),
                ),
            ),
        ),
        caseStudies = listOf(
            CaseStudy("Airbnb", "Travel", "Built MvRx (now Mavericks), its Android framework for immutable screen state, in Kotlin."),
            CaseStudy("Duolingo", "Education", "Moved its Android app to 100% Kotlin and reported gains in productivity, developer happiness and stability."),
        ),
    )

    private val collections = CourseUnit(
        id = "collections",
        title = "Collections & lambdas",
        subtitle = "Say more with fewer lines",
        lessons = listOf(
            Lesson(
                "co-1", "Transform",
                listOf(
                    Choice(
                        "co-1-1", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("[20, 40]", "[10, 30]", "[2, 4]", "[10, 20, 30, 40]"),
                        correctIndex = 0,
                        explanation = "`filter` keeps 2 and 4, then `map` multiplies each by 10.",
                        code = "val nums = listOf(1, 2, 3, 4)\nprintln(nums.filter { it % 2 == 0 }.map { it * 10 })",
                    ),
                    Choice(
                        "co-1-2", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("KOTLIN IS FUN", "[KOTLIN, IS, FUN]", "kotlin is fun", "KOTLINISFUN"),
                        correctIndex = 0,
                        explanation = "`joinToString` takes a separator and an optional transform for each element.",
                        code = "val words = listOf(\"kotlin\", \"is\", \"fun\")\nprintln(words.joinToString(\" \") { it.uppercase() })",
                    ),
                    ArrangeTokens(
                        "co-1-3",
                        "Add up the price of every item.",
                        solution = listOf("val", "total", "=", "items", ".", "sumOf", "{", "it", ".", "price", "}"),
                        distractors = listOf("map", "forEach"),
                        explanation = "`sumOf` maps and sums in one pass, with no mutable accumulator.",
                    ),
                    Choice(
                        "co-1-4", CONCEPT,
                        "Inside `items.map { it.name }`, what is `it`?",
                        listOf("The lambda's only parameter", "The enclosing class", "The previous result", "The whole list"),
                        correctIndex = 0,
                        explanation = "A lambda with one parameter can skip naming it, and Kotlin calls it `it`.",
                    ),
                ),
            ),
            Lesson(
                "co-2", "Group & find",
                listOf(
                    Choice(
                        "co-2-1", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("{veg=2, nonveg=1}", "[veg, nonveg]", "{2=veg, 1=nonveg}", "3"),
                        correctIndex = 0,
                        explanation = "`groupingBy { }.eachCount()` counts elements per key and keeps keys in first-seen order.",
                        code = "val orders = listOf(\"veg\", \"nonveg\", \"veg\")\nprintln(orders.groupingBy { it }.eachCount())",
                    ),
                    Choice(
                        "co-2-2", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("-1", "null", "8", "NoSuchElementException"),
                        correctIndex = 0,
                        explanation = "Nothing is over 10, so `firstOrNull` returns null and the Elvis operator supplies -1. `first { }` would have thrown.",
                        code = "println(listOf(3, 8, 5).firstOrNull { it > 10 } ?: -1)",
                    ),
                    FillBlank(
                        "co-2-3",
                        "Turn a List<User> into a List<String> of names.",
                        code = "val names = users.___ { it.name }",
                        options = listOf("map", "filter", "forEach", "any"),
                        correctIndex = 0,
                        explanation = "`map` transforms each element. `forEach` returns Unit, and `filter` keeps Users.",
                    ),
                    Choice(
                        "co-2-4", CONCEPT,
                        "Why add `asSequence()` to `hugeList.map { … }.filter { … }.first()`?",
                        listOf(
                            "It's lazy: it stops at the first match instead of building full lists at every step",
                            "Sequences run in parallel",
                            "Lists can't be filtered",
                            "It sorts the list first",
                        ),
                        correctIndex = 0,
                        explanation = "List operations build a whole new list at each step. A sequence pushes elements through one at a time and stops early.",
                    ),
                ),
            ),
        ),
        caseStudies = listOf(
            CaseStudy("Pinterest", "Lifestyle", "Moved its Android app from Java to Kotlin starting in 2016, and talked about it at Droidcon NYC that year."),
            CaseStudy("Basecamp 3", "Productivity", "Its Android app is 100% Kotlin; the team reported faster development and better code quality."),
            CaseStudy("Square", "Payments", "An early adopter: its Android developers got the go-ahead for Kotlin in 2015, and it now powers several of its open-source libraries."),
        ),
    )

    private val coroutines = CourseUnit(
        id = "coroutines",
        title = "Coroutines & Flow",
        subtitle = "Async code that reads top to bottom",
        lessons = listOf(
            Lesson(
                "cr-1", "Coroutines",
                listOf(
                    Choice(
                        "cr-1-1", CONCEPT,
                        "What does `suspend` mean in `suspend fun loadFeed()`?",
                        listOf(
                            "It can pause without blocking a thread, so it's called from a coroutine",
                            "It always runs on a background thread",
                            "It's cancelled when the app is paused",
                            "It only runs on the main thread",
                        ),
                        correctIndex = 0,
                        explanation = "`suspend` says the function may pause. Which thread it runs on is decided by the caller's dispatcher.",
                    ),
                    Choice(
                        "cr-1-2", PREDICT_OUTPUT,
                        "In what order are the letters printed?",
                        listOf("B, then A", "A, then B", "Only B", "Only A"),
                        correctIndex = 0,
                        explanation = "`launch` starts a child and returns at once, so B prints first. `runBlocking` waits for its children, so A still prints.",
                        code = "runBlocking {\n    launch {\n        delay(100)\n        println(\"A\")\n    }\n    println(\"B\")\n}",
                    ),
                    Choice(
                        "cr-1-3", FIX_THE_BUG,
                        "This keeps running after the screen closes. Best fix?",
                        listOf(
                            "Use viewModelScope.launch",
                            "Use Thread { }.start()",
                            "Add delay(0) first",
                            "Wrap it in runBlocking",
                        ),
                        correctIndex = 0,
                        explanation = "`GlobalScope` lives as long as the app. `viewModelScope` is cancelled when the ViewModel is cleared, so the work can't leak.",
                        code = "class FeedViewModel : ViewModel() {\n    fun refresh() {\n        GlobalScope.launch {\n            _feed.value = repo.load()\n        }\n    }\n}",
                        highlightLine = 3,
                    ),
                    FillBlank(
                        "cr-1-4",
                        "Switch to the IO dispatcher and return the result.",
                        code = "val user = ___(Dispatchers.IO) { api.fetchUser() }",
                        options = listOf("withContext", "launch", "runBlocking", "async"),
                        correctIndex = 0,
                        explanation = "`withContext` suspends, runs the block on that dispatcher, and returns its value. `launch` and `async` need a scope and don't return the value directly.",
                    ),
                ),
            ),
            Lesson(
                "cr-2", "Flow",
                listOf(
                    Choice(
                        "cr-2-1", CONCEPT,
                        "When does a cold Flow start emitting?",
                        listOf("When something collects it", "As soon as it's created", "Only on the main thread", "When the app starts"),
                        correctIndex = 0,
                        explanation = "A cold flow's block runs for each collector, from the start, only once `collect` is called.",
                    ),
                    Choice(
                        "cr-2-2", PREDICT_OUTPUT,
                        "What does this print?",
                        listOf("4 9", "1 4 9", "2 3", "9 4"),
                        correctIndex = 0,
                        explanation = "Squares are 1, 4, 9, and `filter { it > 1 }` drops the 1.",
                        code = "flowOf(1, 2, 3)\n    .map { it * it }\n    .filter { it > 1 }\n    .collect { print(\"\$it \") }",
                    ),
                    ArrangeTokens(
                        "cr-2-3",
                        "Expose the private MutableStateFlow as a read-only StateFlow.",
                        solution = listOf("val", "state", "=", "_state", ".", "asStateFlow", "()"),
                        distractors = listOf("value", "emit"),
                        explanation = "`asStateFlow()` hides the mutable type, so only the ViewModel can change state.",
                    ),
                    Choice(
                        "cr-2-4", CONCEPT,
                        "In Compose, which collects a StateFlow only while the screen is visible?",
                        listOf(
                            "collectAsStateWithLifecycle()",
                            "collect { } in the composable body",
                            "runBlocking { flow.first() }",
                            "LiveData.observeForever",
                        ),
                        correctIndex = 0,
                        explanation = "It stops collecting below STARTED, so a backgrounded screen doesn't keep upstream work alive.",
                    ),
                ),
            ),
        ),
        caseStudies = listOf(
            CaseStudy("Netflix", "Streaming", "Rebuilt its Android player UI in 100% Kotlin."),
            CaseStudy("Uber", "Ride sharing", "Uses Kotlin for internal tooling such as Gradle plugins and annotation processing."),
        ),
    )

    val course = Course(listOf(nullSafety, dataAndSealed, collections, coroutines))
}
