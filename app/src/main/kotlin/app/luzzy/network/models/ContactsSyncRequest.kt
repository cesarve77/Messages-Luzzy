package app.luzzy.network.models

data class ContactItem(
    val name: String,
    val phone: String
)

data class ContactsSyncRequest(
    val contacts: List<ContactItem>
)
