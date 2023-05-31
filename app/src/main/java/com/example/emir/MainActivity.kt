package com.example.emir

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private lateinit var mainRecyclerView: RecyclerView
    private lateinit var mainAdapter: MainAdapter
    private val categories = arrayOf("Спорт", "Кино", "Музыка")
    private val events: MutableMap<String, MutableList<EventItem>> = mutableMapOf()
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        mainRecyclerView = findViewById(R.id.mainRecyclerView)
        mainAdapter = MainAdapter(categories, events)
        mainRecyclerView.adapter = mainAdapter
        mainRecyclerView.layoutManager = LinearLayoutManager(this)

        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            showAddEventDialog()
        }

        loadSavedEvents()
    }

    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Добавить событие")

        val eventNameEditText = dialogView.findViewById<EditText>(R.id.eventNameEditText)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        val addImageButton = dialogView.findViewById<Button>(R.id.addImageButton)

        val categories = arrayOf("Спорт", "Кино", "Музыка")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categorySpinner.adapter = adapter

        var selectedImage: String? = null

        addImageButton.setOnClickListener {
            openGallery()
        }

        alertDialogBuilder.setPositiveButton("Добавить") { dialog: DialogInterface, which: Int ->
            val eventName = eventNameEditText.text.toString()
            val category = categorySpinner.selectedItem.toString()

            val eventItem = EventItem(category, eventName, selectedImageUri?.toString())

            if (events.containsKey(category)) {
                events[category]?.add(eventItem)
            } else {
                events[category] = mutableListOf(eventItem)
            }

            mainAdapter.notifyDataSetChanged()
            saveEventToDatabase(eventItem)

            dialog.dismiss()
        }

        alertDialogBuilder.setNegativeButton("Отмена") { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            // Обновить изображение в списке мероприятий
            mainAdapter.notifyDataSetChanged()
        }
    }

    private fun loadSavedEvents() {
        val savedEvents = dbHelper.getAllEvents()
        events.clear()

        for (event in savedEvents) {
            val category = event.category
            val name = event.name
            val imageUri = event.imageUri

            val eventItem = EventItem(category, name, imageUri)

            if (events.containsKey(category)) {
                events[category]?.add(eventItem)
            } else {
                events[category] = mutableListOf(eventItem)
            }
        }

        mainAdapter.notifyDataSetChanged()
    }

    private fun saveEventToDatabase(eventItem: EventItem) {
        dbHelper.insertEvent(eventItem.category, eventItem.name, eventItem.imageUri)
    }


    inner class MainAdapter(private val categories: Array<String>, private val events: MutableMap<String, MutableList<EventItem>>) :
        RecyclerView.Adapter<MainAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val categoryTitleTextView: TextView = itemView.findViewById(R.id.categoryTitleTextView)
            val horizontalRecyclerView: RecyclerView = itemView.findViewById(R.id.horizontalRecyclerView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.categoryTitleTextView.text = category

            val categoryEvents = events[category] ?: mutableListOf()
            val horizontalLayoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            holder.horizontalRecyclerView.layoutManager = horizontalLayoutManager

            val horizontalAdapter = HorizontalAdapter(categoryEvents)
            holder.horizontalRecyclerView.adapter = horizontalAdapter
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }

    inner class HorizontalAdapter(private val events: MutableList<EventItem>) :
        RecyclerView.Adapter<HorizontalAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val eventTitleTextView: TextView = itemView.findViewById(R.id.eventTitleTextView)
            val eventImageView: ImageView = itemView.findViewById(R.id.eventImageView)
            val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

            init {
                deleteButton.visibility = View.GONE

                itemView.setOnLongClickListener {
                    deleteButton.visibility = View.VISIBLE
                    true
                }

                deleteButton.setOnClickListener {
                    val eventPosition = adapterPosition
                    val event = events[eventPosition]
                    deleteEventFromDatabase(event) // Удаление из базы данных
                    events.removeAt(eventPosition) // Удаление из списка
                    notifyItemRemoved(eventPosition)
                    notifyDataSetChanged()
                }
            }
        }

        // Остальной код адаптера

        private fun deleteEventFromDatabase(event: EventItem) {
            val db = dbHelper.writableDatabase
            val selection = "${DatabaseHelper.COLUMN_NAME} = ?"
            val selectionArgs = arrayOf(event.name)
            db.delete(DatabaseHelper.TABLE_NAME, selection, selectionArgs)
            db.close()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_horizontal, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val event = events[position]
            holder.eventTitleTextView.text = event.name

            if (event.imageUri != null) {
                Glide.with(holder.itemView)
                    .load(event.imageUri)
                    .into(holder.eventImageView)
            } else {
                holder.eventImageView.setImageResource(R.drawable.ic_launcher_background)
            }

                holder.itemView.setOnClickListener {
                    // Действия при нажатии на событие
                }
            }

        override fun getItemCount(): Int {
            return events.size
        }
    }


}
