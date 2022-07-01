package com.example.chitchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.chitchat.NewMessageActivity.Companion.USER_KEY
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.latest_messages_row.view.*

class LatestMessagesActivity : AppCompatActivity() {

    companion object{
        var currentUser: User?=null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        recyclerview_latest_messages.adapter=adapter
        recyclerview_latest_messages.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))

        //set item click listener on your adapter
        adapter.setOnItemClickListener { item, view ->

            val intent=Intent(this,ChatLogActivity::class.java)

            val row= item as LatestMessageRow
            intent.putExtra(USER_KEY,row.chatPartnerUser)
            startActivity(intent)

        }

        listenForLatestMessages()
        fetchCurrentUser()
        verifyUserIsLoggedIn()

    }
    val adapter= GroupAdapter<GroupieViewHolder>()
    val latestMessagesMap= HashMap<String,ChatMessage>()

    private fun refreshRecyclerViewMessages(){
        adapter.clear()
        latestMessagesMap.values.forEach{
            adapter.add(LatestMessageRow(it))
        }
    }

    private fun listenForLatestMessages() {
        val fromId= FirebaseAuth.getInstance().uid
        val ref= FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")
        ref.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage= snapshot.getValue(ChatMessage::class.java)?:return
                latestMessagesMap[snapshot.key!!]= chatMessage
                refreshRecyclerViewMessages()

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage= snapshot.getValue(ChatMessage::class.java)?:return
                latestMessagesMap[snapshot.key!!]= chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    class LatestMessageRow(val chatMessage: ChatMessage): Item<GroupieViewHolder>(){

        var chatPartnerUser: User?=null

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.message_latest_messages.text=chatMessage.text
            val chatPartnerId: String
            if(chatMessage.fromId==FirebaseAuth.getInstance().uid)
            {
                chatPartnerId=chatMessage.toId
            }
            else{
                chatPartnerId=chatMessage.fromId
            }

            val ref= FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
            ref.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatPartnerUser= snapshot.getValue(User::class.java)
                    viewHolder.itemView.user_latest_messages.text=chatPartnerUser?.username
                    Picasso.get().load(chatPartnerUser?.profileImageUrl).into(viewHolder.itemView.imageView_latestmessages)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })


        }

        override fun getLayout(): Int {
            return R.layout.latest_messages_row
        }

    }

    private fun fetchCurrentUser() {
        val uid= FirebaseAuth.getInstance().uid
        val ref= FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser= snapshot.getValue(User::class.java)
                Log.d("LatestMessageActivity","current user: ${currentUser?.username}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("LatestMessageActivity","failed to get current user")
            }
        })
    }

    // to perform check whether the user is signed in or not
    private fun verifyUserIsLoggedIn() {
        val uid= FirebaseAuth.getInstance().uid
        if(uid==null)
        {
            val intent= Intent(this,RegisterActivity::class.java)
            intent.flags= Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) //when you press back button, this will not take you to register activity page, it will take you out of the app instead
            startActivity(intent)
        }
    }

    //to make the top bar menu buttons work
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // switch case between menu options
        when(item?.itemId)
        {
            R.id.menu_new_message->{
                val intent= Intent(this,NewMessageActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_sign_out->{
                FirebaseAuth.getInstance().signOut()
                val intent= Intent(this,RegisterActivity::class.java)
                intent.flags= Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                //when you press back button, this will not take you to register activity page, it will take you out of the app instead
                startActivity(intent)

            }
        }
        return super.onOptionsItemSelected(item)
    }

    //to set the top bar menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

}