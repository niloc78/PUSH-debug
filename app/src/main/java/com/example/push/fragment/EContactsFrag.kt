package com.example.push.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.push.db.EContact
import com.example.push.adapter.EContactsRecyclerAdapter
import com.example.push.MainActivity
import com.example.push.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.lang.NullPointerException
import java.util.*
import kotlin.collections.ArrayList

class EContactsFrag : Fragment(){

    private var data : ArrayList<EContact> = ArrayList()
    private lateinit var econtactRecyclerView: RecyclerView
    private lateinit var econtactAdapter : EContactsRecyclerAdapter
    private lateinit var econtactsLayoutManager : LinearLayoutManager
    private lateinit var addEContactButton : FloatingActionButton
    private lateinit var defaultBitmap: Bitmap

    private val itemTouchCallback : ItemTouchHelper.SimpleCallback = object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.START or ItemTouchHelper.END or
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPos = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            Collections.swap(data, fromPos, toPosition)
            econtactRecyclerView.adapter?.notifyItemMoved(fromPos, toPosition)

            return true
        }
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            // call update all here
            Log.d("clearview", "called")
            super.clearView(recyclerView, viewHolder)
            for (ec in data) { // sort and update in ui list first
                Log.d("previous prio: ", "" + ec.priority)
                ec.priority = data.indexOf(ec)
                Log.d("updated prio: ",  "to " + ec.priority)
            }
            runBlocking {
                val dao = (requireActivity() as MainActivity).db.econtactDAO()
                dao.updateAll(*data.toTypedArray())
                parentFragmentManager.apply {
                    val result = Bundle()
                    result.putStringArray("eContactNums", buildNumsArray())
                    this.setFragmentResult("eContactsChanged", result)
                }
                //econtactRecyclerView.adapter?.notifyDataSetChanged()
            }

        }


        override fun isLongPressDragEnabled(): Boolean {
            return true
        }


        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END
            val swipeFlags = ItemTouchHelper.LEFT
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val pos = viewHolder.adapterPosition

            // call delete here
            runBlocking {
                val dao = (requireActivity() as MainActivity).db.econtactDAO()
                dao.deleteAll(data.removeAt(pos))
                econtactRecyclerView.adapter?.notifyItemRemoved(pos)
            }

        }

    }

    private fun buildNumsArray() : Array<String> {
        val arrayList = ArrayList<String>()
        for (ec in data) {
            arrayList.add(ec.contactPhone!!)
        }
        return arrayList.toTypedArray()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.econtacts_frag_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        parentFragmentManager.apply {
            this.setFragmentResultListener("AlertFragmentCreated", this@EContactsFrag,
                { requestKey, result ->
                    //previewMessage.text = result.getString("previewContent")
                    if(result.getBoolean("AlertFragmentCreated")) {
                        val sendResult = Bundle()
                        sendResult.putStringArray("eContactNums", buildNumsArray())
                        this.setFragmentResult("eContactsChanged", sendResult)
                        Log.d("received Result", "AlertFragmentCreated: " + result.getBoolean("AlertFragmentCreated"))
                    }
                })
        }

    }

    private fun loadFromDb()  = runBlocking {
        val dao = (requireActivity() as MainActivity).db.econtactDAO()
        val results = dao.getSortedListByPriority()
        data.addAll(results)
        econtactRecyclerView.adapter?.notifyDataSetChanged()
        Log.d("loaded from db", "Success")
    }

    private fun initViews(v : View) {
        econtactRecyclerView = v.findViewById(R.id.econtacts_recycler_view)
        econtactsLayoutManager = LinearLayoutManager(context)
        Log.d("data size", "" + data.size)
        econtactAdapter = EContactsRecyclerAdapter(data)
        econtactRecyclerView.apply {
            layoutManager = econtactsLayoutManager
            adapter = econtactAdapter
            isNestedScrollingEnabled = false
        }
        //econtactAdapter.notifyDataSetChanged()

        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(econtactRecyclerView)
        addEContactButton = v.findViewById(R.id.add_contact_button)
        addEContactButton.setOnClickListener {
            if (!checkPermission()) {
                mPermissionResult.launch(Manifest.permission.READ_CONTACTS)
            } else {
                launchContactChooserActivity()
            }
        }

        defaultBitmap = BitmapFactory.decodeResource(requireContext().resources,
            R.drawable.default_contact_pic
        )
        loadFromDb()
    }

    private val mPermissionResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        when (result) {
            true -> {
                Log.d("Read Contact Permission", "Granted")
                launchContactChooserActivity()
            }
            else -> {
                Log.d("Read Contact Permission", "Denied")
            }
        }

    }

    private fun launchContactChooserActivity() {
        Intent(Intent.ACTION_PICK).also { intent ->
            intent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            contactChooseLauncher.launch(intent)
        }
    }

    private val contactChooseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("choose contact result", "RESULT OK")
             //check if exists already and add to recycler view
            val contactUri = result.data?.data
            val contactMetaDataAsEContactObject = getContactMetaDataFromUriAsEContactObject(contactUri)

            if (contactMetaDataAsEContactObject != null) {
                if (econtactAlreadyExists(contactMetaDataAsEContactObject)) {
                    Toast.makeText(requireContext(), "Contact Already Exists", Toast.LENGTH_SHORT).show()
                } else {
                    //call insert here
                    runBlocking {
                        data.add(contactMetaDataAsEContactObject)
                        Log.d("Added contact object: ", "contactId: " + contactMetaDataAsEContactObject.contactId +
                                " contactName: " + contactMetaDataAsEContactObject.contactName + " contactPhone: " + contactMetaDataAsEContactObject.contactPhone +
                                " priority: " + contactMetaDataAsEContactObject.priority)

                        val dao = (requireActivity() as MainActivity).db.econtactDAO()
                        val idsInserted = dao.insertAll(contactMetaDataAsEContactObject)
                        Log.d("ids inserted to db: ", idsInserted.joinToString())
                        econtactAdapter.notifyItemInserted(data.lastIndex)
                        parentFragmentManager.apply {
                            val result = Bundle()
                            result.putStringArray("eContactNums", buildNumsArray())
                            this.setFragmentResult("eContactsChanged", result)
                        }
                    }

                }
            } else {
                Log.d("ERROR ADDING Contact", "cursor was null")
            }
        } else { // none selected
            Log.d("choose contact result", "Result not ok")
        }
    }

    private fun econtactAlreadyExists(eContact: EContact) : Boolean {
        for (ec in data) {
            if (eContact.contactPhone == ec.contactPhone) {
                return true
            }
        }
        return false
    }


    private fun getContactMetaDataFromUriAsEContactObject(contactUri : Uri?) : EContact? {
        var index : Int
        var contactPic = defaultBitmap
        val cursor =
            contactUri?.let { requireContext().contentResolver.query(it, null, null, null) }
        if (cursor != null && cursor.moveToFirst()) {
            index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val contactId = cursor.getInt(index)
            Log.d("contactID", "" + contactId)
            index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val contactName = cursor.getString(index)
            Log.d("Contact Name", contactName)


            index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val contactPhone = cursor.getString(index)
            Log.d("Contact Phone", contactPhone)

            index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val photoUri = cursor.getString(index)
            try {
                val uri = Uri.parse(photoUri)
                contactPic = BitmapFactory.decodeStream(openDisplayPhotoFromUri(uri))

            } catch (exception : NullPointerException) {
                Log.d("exception", "caught")
                //contactPic = BitmapFactory.decodeResource(requireContext().resources, R.drawable.default_contact_pic)
                //do nothing
            }

            return EContact(contactId, contactPic, contactName, contactPhone, data.lastIndex + 1)
//            return linkedMapOf("contactPic" to contactPic, "contactName" to contactName, "contactPhone" to contactPhone)
        }
        return null
//        return linkedMapOf("contactPic" to contactPic, "contactName" to "N/A", "contactPhone" to "N/A")
    }


    private fun openDisplayPhotoFromUri(photoUri: Uri) : InputStream? {
        return try {
            val fd = requireContext().contentResolver.openAssetFileDescriptor(photoUri, "r")
            fd?.createInputStream()

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun checkPermission() : Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    override fun toString(): String {
        return "CustomizeFragment"
    }
}