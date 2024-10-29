package com.example.aviagerman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.room.Room
import com.example.aviagerman.dao.UserDao
import com.example.aviagerman.database.AppDatabase
import com.example.aviagerman.entity.User
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.aviagerman.dao.FlightDao
import com.example.aviagerman.entity.Flight
import com.google.accompanist.pager.ExperimentalPagerApi
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.aviagerman.dao.BookingDao
import com.example.aviagerman.entity.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@ExperimentalPagerApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    //  external fun stringFromJNI(): String
    init {
        System.loadLibrary("aviagerman-lib")
    }
    private external fun validatePriceNative(price: String): Boolean
    private fun validatePrice(price: String): Boolean {
        return validatePriceNative(price) // Вызов JNI функции
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "aviagerman-database"
        ).build()

        val coroutineScope = CoroutineScope(Dispatchers.Main)
        coroutineScope.launch {
            val userDao = db.userDao()
            val flightDao = db.flightDao()
            val bookingDao = db.bookingDao()

            setContent {
                MyApp(
                    userDao = userDao,
                    flightDao = flightDao,
                    bookingDao = bookingDao,
                    validatePrice = this@MainActivity::validatePrice // Передача функции
                )
            }
        }
    }
}




@Composable
@ExperimentalFoundationApi
@ExperimentalPagerApi
fun MyApp(
    userDao: UserDao,
    flightDao: FlightDao,
    bookingDao: BookingDao,
    validatePrice: (String) -> Boolean // Добавьте этот параметр
) {
    val pagerState = rememberPagerState(initialPage = 0)

    val tabs = listOf("Поиск билетов", "Мои брони", "Профиль")
    val coroutineScope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }

    LaunchedEffect(nickname) {
        userRole = userDao.getUserRole(nickname) ?: ""
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(pagerState.currentPage) { newIndex ->
                coroutineScope.launch {
                    pagerState.scrollToPage(newIndex)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            HorizontalPager(
                state = pagerState,
                count = tabs.size
            ) { page ->
                when (page) {
                    0 -> TicketSearchScreen(
                        userRole = userRole,
                        flightDao = flightDao,
                        onNavigateToLogin = {
                            coroutineScope.launch {
                                pagerState.scrollToPage(2)
                            }
                        },
                        userDao = userDao,
                        bookingDao = bookingDao,
                        nickname = nickname,
                        validatePrice = validatePrice // Передача функции
                    )
                    1 -> BookingScreen(bookingDao = bookingDao, userDao = userDao, flightDao = flightDao, nickname = nickname)
                    2 -> ProfileScreen(userDao = userDao, nickname = nickname, onNicknameChange = { newNickname -> nickname = newNickname }, flightDao = flightDao, onNavigateToTickets = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(0)
                        }
                    })
                }
            }
        }
    }
}



@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Поиск билетов") },
            label = { Text("Билеты") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Брони") },
            label = { Text("Мои брони") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Профиль") },
            label = { Text("Профиль") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
    }
}


@Composable
fun TicketSearchScreen(
    userRole: String,
    flightDao: FlightDao,
    onNavigateToLogin: () -> Unit,
    userDao: UserDao,
    bookingDao: BookingDao,
    nickname: String,
    validatePrice: (String) -> Boolean
) {
    var showAddFlightDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var availableFlights by remember { mutableStateOf<List<Flight>>(emptyList()) }
    var isAllFlightsShown by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedFlight by remember { mutableStateOf<Flight?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Поиск рейсов", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Показ всех рейсов или фильтр по дате
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    val flights = flightDao.getAllFlights()
                    withContext(Dispatchers.Main) {
                        availableFlights = flights
                        isAllFlightsShown = true
                    }
                }
            }) {
                Text("Показать все рейсы")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                DatePickerButton(selectedDate) { date ->
                    selectedDate = date
                    isAllFlightsShown = false
                    coroutineScope.launch(Dispatchers.IO) {
                        val flights = flightDao.getAllFlights().filter { it.date == date }
                        withContext(Dispatchers.Main) {
                            availableFlights = flights
                        }
                    }
                }
                if (selectedDate.isNotEmpty()) {
                    Text(text = selectedDate, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Отображение рейсов
        if (availableFlights.isNotEmpty()) {
            Text(
                text = if (isAllFlightsShown) "Все доступные рейсы" else "Доступные рейсы на $selectedDate",
                style = MaterialTheme.typography.titleMedium
            )
            availableFlights.forEach { flight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (userRole.isEmpty()) {
                                onNavigateToLogin()
                            } else {
                                selectedFlight = flight
                                showConfirmationDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(text = "${flight.date}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "${flight.departure} - ${flight.arrival}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Время: ${flight.time}, Цена: ${flight.price} руб.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (userRole == "admin") {
                        IconButton(onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                flightDao.deleteFlight(flight)
                                withContext(Dispatchers.Main) {
                                    availableFlights = availableFlights.filter { it.id != flight.id }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить рейс")
                        }
                    }
                }
            }
        } else if (selectedDate.isNotEmpty()) {
            Text("Нет рейсов на выбранную дату", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (userRole == "admin") {
            Button(onClick = { showAddFlightDialog = true }) {
                Text("Добавить рейс")
            }
        }

        if (showConfirmationDialog) {
            ConfirmationDialog(
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        selectedFlight?.let { flight ->
                            // Создаем бронирование и сохраняем его в базу данных
                            val userId = userDao.getUserIdByNickname(nickname) ?: return@launch
                            val booking = Booking(
                                userId = userId,
                                flightId = flight.id,
                                bookingDate = selectedDate,
                                status = "Подтверждено"
                            )
                            bookingDao.insertBooking(booking)
                        }
                        showConfirmationDialog = false
                    }
                },
                onDismiss = { showConfirmationDialog = false }
            )
        }

        if (showAddFlightDialog) {
            AddFlightDialog(onDismiss = { showAddFlightDialog = false }, userDao = userDao, flightDao = flightDao, validatePrice = validatePrice)
        }
    }
}



@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтвердить бронирование") },
        text = { Text("Вы уверены, что хотите подтвердить бронирование?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Да")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Нет")
            }
        }
    )
}

class BookingAdapter(
    private val bookings: List<Booking>,
    private val flightDetails: Map<Int, Flight>,
    private val onBookingLongPress: (Booking) -> Unit,
    private val onBookingSelected: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flightInfo: TextView = itemView.findViewById(R.id.flightInfo)
        val bookingStatus: TextView = itemView.findViewById(R.id.bookingStatus)
        val bookingDate: TextView = itemView.findViewById(R.id.bookingDate)
        val itemLayout: LinearLayout = itemView.findViewById(R.id.itemLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        val flight = flightDetails[booking.flightId]
        holder.flightInfo.text = "${flight?.departure} - ${flight?.arrival}"
        holder.bookingStatus.text = "Статус: ${booking.status}"
        holder.bookingDate.text = "Дата бронирования: ${flight?.date}"
        holder.itemView.setOnLongClickListener {
            onBookingLongPress(booking)
            onBookingSelected(booking)
            true
        }
    }

    override fun getItemCount(): Int {
        return bookings.size
    }
}


@Composable
fun BookingScreen(userDao: UserDao, bookingDao: BookingDao, flightDao: FlightDao, nickname: String) {
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var flightDetails by remember { mutableStateOf<Map<Int, Flight>>(emptyMap()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var bookingToDelete by remember { mutableStateOf<Booking?>(null) }
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Загружаем бронирования и соответствующие данные о рейсах
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val userId = userDao.getUserIdByNickname(nickname) ?: return@launch
            val userBookings = bookingDao.getBookingsByUserId(userId)

            // Получаем данные о рейсах для каждого бронирования
            val flightsMap = userBookings.associateBy(
                { it.flightId },
                { flightDao.getFlightById(it.flightId) }
            ).filterValues { it != null } as Map<Int, Flight>

            withContext(Dispatchers.Main) {
                bookings = userBookings
                flightDetails = flightsMap
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Мои бронирования", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (bookings.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    RecyclerView(context).apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = BookingAdapter(bookings, flightDetails, { booking ->
                            bookingToDelete = booking
                            showDeleteConfirmation = true
                        }, { booking ->
                            selectedBooking = booking
                        })
                    }
                }
            )
        } else {
            if (nickname.isEmpty()){
                Text("Вы не вошли в акаунт.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("У вас пока нет бронирований.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showDeleteConfirmation) {
        ConfirmationDeleteDialog(
            onConfirm = {
                bookingToDelete?.let { booking ->
                    coroutineScope.launch(Dispatchers.IO) {
                        bookingDao.deleteBookingById(booking.id)
                        withContext(Dispatchers.Main) {
                            bookings = bookings.filter { it.id != booking.id }
                            showDeleteConfirmation = false
                        }
                    }
                }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}


@Composable
fun ConfirmationDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтвердить удаление") },
        text = { Text("Вы уверены, что хотите удалить эту бронь?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Да")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Нет")
            }
        }
    )
}


//@Composable
//fun FlightBookingCard(flight: Flight, booking: Booking, bookingDao: BookingDao, onBookingDeleted: (Int) -> Unit) {
//    var showMenu by remember { mutableStateOf(false) }
//
//    val longPressModifier = Modifier.pointerInput(Unit) {
//        detectTapGestures(
//            onLongPress = {
//                showMenu = true
//            }
//        )
//    }
//
//    Card(
//        modifier = longPressModifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        elevation = CardDefaults.cardElevation(4.dp),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = "${flight.departure} - ${flight.arrival}",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//                Text(
//                    text = flight.time,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//            }
//            Text(
//                text = "Статус: ${booking.status}",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onPrimaryContainer
//            )
//            Text(
//                text = "Дата бронирования: ${booking.bookingDate}",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onPrimaryContainer
//            )
//        }
//    }
//
//    if (showMenu) {
//        MenuDialog(
//            onDismiss = { showMenu = false },
//            booking = booking,
//            onConfirmDelete = { bookingId ->
//                CoroutineScope(Dispatchers.IO).launch {
//                    bookingDao.deleteBookingById(bookingId)
//                }
//                onBookingDeleted(bookingId)
//                showMenu = false
//            }
//        )
//    }
//}

//@Composable
//fun MenuDialog(onDismiss: () -> Unit, booking: Booking, onConfirmDelete: (Int) -> Unit) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Меню") },
//        text = { Text("Вы желаете удалить бронь ${booking.id}:") },
//        confirmButton = {
//            Button(onClick = {
//                onConfirmDelete(booking.id)
//            }) {
//                Text("Да")
//            }
//        },
//        dismissButton = {
//            Button(onClick = onDismiss) {
//                Text("Закрыть")
//            }
//        }
//    )
//}


//@Composable
//fun NoFlightInfoCard(booking: Booking) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        elevation = CardDefaults.cardElevation(4.dp),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.errorContainer
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text(
//                text = "Информация о рейсе не найдена",
//                style = MaterialTheme.typography.titleMedium,
//                color = MaterialTheme.colorScheme.onErrorContainer
//            )
//            Text(
//                text = "Статус: ${booking.status}",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onErrorContainer
//            )
//            Text(
//                text = "Дата бронирования: ${booking.bookingDate}",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onErrorContainer
//            )
//        }
//    }
//}





@Composable
fun AddFlightDialog(onDismiss: () -> Unit, userDao: UserDao, flightDao: FlightDao,validatePrice: (String) -> Boolean) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background
        ) {
            AddFlightScreen(
                userDao = userDao,
                userRole = "admin",
                flightDao = flightDao,
                onFlightAdded = onDismiss,
                validatePrice = validatePrice
            )
        }
    }
}




@Composable
fun AddFlightScreen(
    userDao: UserDao,
    userRole: String,
    flightDao: FlightDao,
    onFlightAdded: () -> Unit,
    validatePrice: (String) -> Boolean
) {
    var flightNumber by remember { mutableStateOf("") }
    var departure by remember { mutableStateOf("") }
    var arrival by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isDataConfirmed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Добавить рейс", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = flightNumber,
            onValueChange = { flightNumber = it },
            label = { Text("Номер рейса") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = departure,
            onValueChange = { departure = it },
            label = { Text("Откуда") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = arrival,
            onValueChange = { arrival = it },
            label = { Text("Куда") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DatePickerButton(date) { selectedDate ->
            date = selectedDate
        }

        Spacer(modifier = Modifier.height(16.dp))

        TimePicker(
            selectedTime = time,
            onTimeSelected = { selectedTime -> time = selectedTime }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = price,
            onValueChange = {
                price = it
                // Валидация цены
                if (!validatePrice(price)) {
                    errorMessage = "Цена должна содержать только числа."
                } else {
                    errorMessage = ""
                }
            },
            label = { Text("Цена") },
            isError = errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                isDataConfirmed = flightNumber.isNotEmpty() && departure.isNotEmpty() &&
                        arrival.isNotEmpty() && date.isNotEmpty() &&
                        time.isNotEmpty() && price.isNotEmpty() &&
                        validatePrice(price) // Проверка валидности цены
                if (!isDataConfirmed) {
                    errorMessage = "Пожалуйста, подтвердите данные и заполните все поля корректно."
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDataConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        ) {
            Text("Подтвердить данные")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (isDataConfirmed) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val flight = Flight(
                            flightNumber = flightNumber,
                            departure = departure,
                            arrival = arrival,
                            date = date,
                            time = time,
                            price = price.toDoubleOrNull() ?: 0.0
                        )
                        val insertedId = flightDao.insertFlight(flight)
                        Log.d("MainActivity", "Flight inserted with ID: $insertedId")
                        withContext(Dispatchers.Main) {
                            onFlightAdded()
                            isDataConfirmed = false
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error inserting flight", e)
                        withContext(Dispatchers.Main) {
                            errorMessage = "Ошибка при добавлении рейса: ${e.message}"
                        }
                    }
                }
            }
        }) {
            Text("Внести в базу")
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}


@Composable
fun TimePicker(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val hour = remember { mutableStateOf(12) } // Начальный час
    val minute = remember { mutableStateOf(0) } // Начальная минута
    val openDialog = remember { mutableStateOf(false) }

    Button(
        onClick = { openDialog.value = true }) {
        Text(text = if (selectedTime.isEmpty()) "Выберите время" else selectedTime)
    }

    if (openDialog.value) {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                hour.value = selectedHour
                minute.value = selectedMinute
                onTimeSelected(String.format("%02d:%02d", selectedHour, selectedMinute))
                openDialog.value = false
            },
            hour.value,
            minute.value,
            true
        ).show()
    }
}

@Composable
fun DatePickerButton(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val showDialog = remember { mutableStateOf(false) }

    Button(onClick = { showDialog.value = true }) {
        Text("Выберите дату $selectedDate")
    }

    if (showDialog.value) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = "$dayOfMonth/${month + 1}/$year"
                onDateSelected(selected)
                showDialog.value = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

@Composable
fun ProfileScreen(userDao: UserDao, flightDao: FlightDao, nickname: String, onNicknameChange: (String) -> Unit, onNavigateToTickets: () -> Unit) {
    var isLoggedIn by remember { mutableStateOf(nickname.isNotEmpty()) }
    var showLogin by remember { mutableStateOf(true) }
    var userRole by remember { mutableStateOf("") }

    LaunchedEffect(nickname) {
        userRole = userDao.getUserRole(nickname) ?: ""
    }

    if (isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Профиль", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Имя пользователя: $nickname")
            Text("Роль: $userRole")

            Button(onClick = {
                isLoggedIn = false
                onNicknameChange("")
            }) {
                Text("Выйти")
            }
        }
    } else {
        if (showLogin) {
            LoginScreen(userDao, onLoginSuccess = { loginNickname ->
                isLoggedIn = true
                onNicknameChange(loginNickname)
                showLogin = false
            }, onNavigateToTickets = onNavigateToTickets)
        } else {
            RegistrationScreen(userDao, onRegisterSuccess = { registerNickname ->
                isLoggedIn = true
                onNicknameChange(registerNickname)
                showLogin = true
            }, onNavigateToTickets = onNavigateToTickets)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { showLogin = !showLogin }) {
            Text(if (showLogin) "Нет аккаунта? Зарегистрируйтесь" else "Уже есть аккаунт? Войти")
        }
    }
}



@Composable
fun RegistrationScreen(userDao: UserDao, onRegisterSuccess: (String) -> Unit, onNavigateToTickets: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Регистрация", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Никнейм") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (nickname.isNotEmpty() && password.isNotEmpty()) {
                coroutineScope.launch {
                    val existingUser = userDao.getUser(nickname, password)
                    if (existingUser == null) {
                        val newUser = User(nickname = nickname, password = password, role = "user")
                        userDao.insertUser(newUser)
                        onRegisterSuccess(nickname)
                        onNavigateToTickets()
                    } else {
                        errorMessage = "Такой пользователь уже существует"
                    }
                }
            } else {
                errorMessage = "Пожалуйста, заполните все поля"
            }
        }) {
            Text("Зарегистрироваться")
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun LoginScreen(userDao: UserDao, onLoginSuccess: (String) -> Unit, onNavigateToTickets: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Вход", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Никнейм") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (nickname.isNotEmpty() && password.isNotEmpty()) {
                coroutineScope.launch {
                    val user = userDao.getUser(nickname, password)
                    if (user != null) {
                        onLoginSuccess(nickname)
                        onNavigateToTickets()
                    } else {
                        errorMessage = "Неверный никнейм или пароль"
                    }
                }
            } else {
                errorMessage = "Пожалуйста, заполните все поля"
            }
        }) {
            Text("Войти")
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }

}

class MockUserDao : UserDao {
    private val users = mutableListOf<User>()

    override suspend fun insertUser(user: User) {
        users.add(user)
    }

    override suspend fun getUser(nickname: String, password: String): User? {
        return users.find { it.nickname == nickname && it.password == password }
    }

    override suspend fun getUsersByRole(role: String): List<User> {
        return users.filter { it.role == role }
    }

    override suspend fun getUserRole(nickname: String): String? {
        return users.find { it.nickname == nickname }?.role
    }

    override suspend fun getUserIdByNickname(nickname: String): Int? {
        return users.find {it.nickname == nickname}?.id
    }
}

class MockFlightDao : FlightDao {
    private val flights = mutableListOf<Flight>()

    override suspend fun insertFlight(flight: Flight) {
        flights.add(flight)
    }

    override suspend fun getAllFlights(): List<Flight> {
        return flights
    }

    override suspend fun getFlightById(flightId: Int): Flight? {
        return flights.find { it.id == flightId }
    }

    override suspend fun deleteFlight(flight: Flight) {
        flights.remove(flight)
    }
}

class MockBookingDao : BookingDao {
    private val bookings = mutableListOf<Booking>()

    override suspend fun insertBooking(booking: Booking) {
        bookings.add(booking.copy(id = (bookings.maxOfOrNull { it.id } ?: 0) + 1)) // Генерация ID
    }

    override suspend fun getBookingsByUserId(userId: Int): List<Booking> {
        return bookings.filter { it.userId == userId }
    }

    override suspend fun getBookingsByFlightId(flightId: Int): List<Booking> {
        return bookings.filter { it.flightId == flightId }
    }

    override suspend fun deleteBooking(booking: Booking) {
        bookings.removeIf { it.id == booking.id }
    }

    override suspend fun deleteBookingById(id: Int) {
        bookings.removeIf { it.id == id }
    }

    override suspend fun updateBookingStatus(bookingId: Int, status: String) {
        val booking = bookings.find { it.id == bookingId }
        booking?.let {
            bookings[bookings.indexOf(it)] = it.copy(status = status)
        }
    }
}


@Preview(showBackground = true)
@Composable
@ExperimentalPagerApi
@ExperimentalFoundationApi
fun DefaultPreview() {
    MyApp(userDao = MockUserDao(), flightDao = MockFlightDao(), bookingDao = MockBookingDao(), validatePrice = { price: String -> true })
}
