package com.example.criminalintent.crimeFragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.DatePickerDialogFragment
import com.example.criminalintent.database.Crime
import com.example.criminalintent.R
import com.example.criminalintent.crimeListFragment.KEY_ID
import com.example.criminalintent.utils.getScaledBitmap
import java.io.File
import java.util.*

/**
 * //TODO
 */

const val CRIME_DATE_KEY = "crimeDate"
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2

class CrimeFragment : Fragment(),DatePickerDialogFragment.DatePickerCallback {


    private lateinit var titleEditText: EditText
    private lateinit var dateBtn:Button
    private lateinit var isSolvedCheckBox: CheckBox
    private lateinit var reportBtn:Button
    private lateinit var suspectBtn:Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private lateinit var crime: Crime

    private val dateFormat = "yyyy / MM / dd"

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private val fragmentViewModel by lazy { ViewModelProvider(this)
        .get(CrimeFragmentViewModel::class.java) }





    private val getResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()){

        }

    override fun onStart() {
        super.onStart()

        photoButton.setOnClickListener {
            // launch the cam
            getResult.launch(photoUri)

        }


        suspectBtn.setOnClickListener {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

                startActivityForResult(pickContactIntent, REQUEST_CONTACT)


        }


        reportBtn.setOnClickListener {

            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "CriminalIntent Crime Report")
            }.also {
                val chooserIntent =
                    Intent.createChooser(it," send_report")
                startActivity(chooserIntent)
            }

        }

        dateBtn.setOnClickListener {

            val args = Bundle()
            args.putSerializable(CRIME_DATE_KEY,crime.date)

            val datePicker = DatePickerDialogFragment()

            datePicker.arguments = args
            datePicker.setTargetFragment(this,0)
            datePicker.show(this.parentFragmentManager,"date picker")
        }


        val textWatcher = object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // i will do nothing
            }

            override fun onTextChanged(sssss: CharSequence?, p1: Int, p2: Int, p3: Int) {
                crime.title = sssss.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
                // i will do nothing
            }

        }

        titleEditText.addTextChangedListener(textWatcher)

       isSolvedCheckBox.setOnCheckedChangeListener { _, isChecked ->

        crime.isSolved = isChecked

       }
    }


    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())

            //val bitmap2 = BitmapFactory.decodeFile(photoFile.path)
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)

        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        fragmentViewModel.crimeLiveData.observe(
            viewLifecycleOwner, {
                it?.let {
                    crime = it

                    photoFile = fragmentViewModel.getPhotoFile(it)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "com.example.criminalintent",
                        photoFile)

                    updatePhotoView()

                    titleEditText.setText(it.title)
                    dateBtn.text = it.date.toString()
                    isSolvedCheckBox.isChecked = it.isSolved
                    if (crime.suspect.isNotBlank()){
                        suspectBtn.text = crime.suspect
                    }



                }

            }
        )

    }



    override fun onDateSelected(date: Date) {
        crime.date = date
        dateBtn.text = date.toString()
    }

    override fun onStop() {
        super.onStop()
        fragmentViewModel.saveUpdate(crime)
    }

    override fun onDetach() {
        super.onDetach()
//        requireActivity().revokeUriPermission(
//            photoUri,
//            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()

        val crimeId = arguments?.getSerializable(KEY_ID) as UUID
        fragmentViewModel.loadCrime(crimeId)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if (resultCode != Activity.RESULT_OK){
            return
        }

        if (requestCode == REQUEST_CONTACT && data != null){
            val contactsURI = data.data


            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

            val cursor = contactsURI?.let {
                requireActivity().contentResolver.query(
                    it,queryFields,null,null,null
                )
            }

            cursor?.let {cursor->
                cursor.use {

                    if (it.count == 0 ){ return }

                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    // until here


                    // there is a bug can you solve it
                    val crimeId = arguments?.getSerializable(KEY_ID) as UUID
                    fragmentViewModel.loadCrime(crimeId)
                    crime.id = crimeId

                    fragmentViewModel.saveUpdate(crime)
                    suspectBtn.text = suspect
                }


            }



        }


    }


    private fun getCrimeReport():String{

        val solvedString = if (crime.isSolved){
            "the crime has been solved"
        }else{
            " the crime has not been solved "
        }


        val dateString = DateFormat.format(dateFormat,crime.date)


        val suspectString = if (crime.suspect.isBlank()){
            "there is no any suspect"
        }else{
            "the suspect is ${crime.suspect}"
        }

        return "$solvedString and the date of the crime is $dateString and $suspectString "

    }


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_crime,container,false)
        init(view)


        dateBtn.apply {
            text = crime.date.toString()
        }

        return view
    }


    private fun init(view: View) {
        titleEditText = view.findViewById(R.id.crime_title)
        dateBtn = view.findViewById(R.id.crime_date)
        isSolvedCheckBox = view.findViewById(R.id.crime_solved)
        reportBtn = view.findViewById(R.id.crime_report)
        suspectBtn = view.findViewById(R.id.crime_suspect)
        photoButton = view.findViewById(R.id.crime_camera)
        photoView = view.findViewById(R.id.crime_photo)
    }
}