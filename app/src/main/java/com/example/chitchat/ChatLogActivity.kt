package com.example.chitchat

import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import java.sql.Timestamp

class ChatLogActivity : AppCompatActivity() {

    val adapter= GroupAdapter<GroupieViewHolder>()
    var toUser:User?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        setBackButton()
        //val username= intent.getStringExtra(NewMessageActivity.USER_KEY)
        if(adapter == null)
            Log.d("ChatLog","No adapter")
        recyclerview_chatlog.adapter=adapter
        toUser= intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)

        supportActionBar?.title= toUser?.username

        //setUpDummyData()
        listenForMessages()
        send_button_chatlog.setOnClickListener{
            performSendMessage()
        }

    }

    private fun setBackButton() {

    }


    private fun listenForMessages() {
        val fromId= FirebaseAuth.getInstance().uid
        val toId= toUser?.uid
        val ref= FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        ref.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage= snapshot.getValue(ChatMessage::class.java)
                if (chatMessage!=null) {
                    if(chatMessage.fromId==FirebaseAuth.getInstance().uid)
                    {
                        val currentUser= LatestMessagesActivity.currentUser?:return
                        Log.d("User","${currentUser.username}")
                        adapter.add(ChatFromItem(chatMessage.text,currentUser))
                    }
                    else
                    {
                        adapter.add(ChatToItem(chatMessage.text,toUser!!))
                    }
                }

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {


            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }



    private fun performSendMessage() {

        val text=edittext_chatlog.text.toString()
        val fromId= FirebaseAuth.getInstance().uid
        val user= intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId= user!!.uid

        if(fromId==null) return

//        val reference= FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference= FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference= FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()
        val chatMessage= ChatMessage(reference.key!!,text,fromId,toId,System.currentTimeMillis()/1000)
        reference.setValue(chatMessage).addOnSuccessListener {
            Log.d("ChatLogActivity","Successfully updated")
            edittext_chatlog.text.clear()
            recyclerview_chatlog.scrollToPosition(adapter.itemCount - 1)
        }
            .addOnFailureListener {
                Log.d("ChatLogActivity","Failed updated:${it.toString()}")
            }
        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)
        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }


}

class ChatMessage(val id:String, val text: String, val fromId:String, val toId:String, val timestamp:Long){
    constructor():this("", "", "", "", -1)
}

class ChatFromItem(val text: String, val user: User): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_from.text= text
        //to load user image
        if(user!=null)
        {
            //val uri= user.profileImageUrl
            //val targetImageView= viewHolder.itemView.imageview_chat_from_row
            //Picasso.get().load(uri).into(targetImageView)
        }

    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

}

class ChatToItem(val text: String,val user: User): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_to.text=text

        //to load user image
        //val uri= user.profileImageUrl
        //val targetImageView= viewHolder.itemView.imageview_chat_to_row
        //Picasso.get().load(uri).into(targetImageView)

    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

}