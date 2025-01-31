package chat.simplex.app.views.chat.group

import SectionCustomFooter
import SectionDivider
import SectionItemView
import SectionSpacer
import SectionView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.app.R
import chat.simplex.app.model.*
import chat.simplex.app.ui.theme.*
import chat.simplex.app.views.chat.ChatInfoToolbarTitle
import chat.simplex.app.views.helpers.*
import chat.simplex.app.views.usersettings.SettingsActionItem

@Composable
fun AddGroupMembersView(groupInfo: GroupInfo, chatModel: ChatModel, close: () -> Unit) {
  val selectedContacts = remember { mutableStateListOf<Long>() }
  val selectedRole = remember { mutableStateOf(GroupMemberRole.Member) }

  BackHandler(onBack = close)
  AddGroupMembersLayout(
    groupInfo = groupInfo,
    contactsToAdd = getContactsToAdd(chatModel),
    selectedContacts = selectedContacts,
    selectedRole = selectedRole,
    inviteMembers = {
      withApi {
        for (contactId in selectedContacts) {
          val member = chatModel.controller.apiAddMember(groupInfo.groupId, contactId, selectedRole.value)
          if (member != null) {
            chatModel.upsertGroupMember(groupInfo, member)
          } else {
            break
          }
        }
        close.invoke()
      }
    },
    clearSelection = { selectedContacts.clear() },
    addContact = { contactId -> if (contactId !in selectedContacts) selectedContacts.add(contactId) },
    removeContact = { contactId -> selectedContacts.removeIf { it == contactId } },
  )
}

fun getContactsToAdd(chatModel: ChatModel): List<Contact> {
  val memberContactIds = chatModel.groupMembers
    .filter { it.memberCurrent }
    .mapNotNull { it.memberContactId }
  return chatModel.chats
    .asSequence()
    .map { it.chatInfo }
    .filterIsInstance<ChatInfo.Direct>()
    .map { it.contact }
    .filter { it.contactId !in memberContactIds }
    .sortedBy { it.displayName.lowercase() }
    .toList()
}

@Composable
fun AddGroupMembersLayout(
  groupInfo: GroupInfo,
  contactsToAdd: List<Contact>,
  selectedContacts: SnapshotStateList<Long>,
  selectedRole: MutableState<GroupMemberRole>,
  inviteMembers: () -> Unit,
  clearSelection: () -> Unit,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.Start,
  ) {
    AppBarTitle(stringResource(R.string.button_add_members))
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center
    ) {
      ChatInfoToolbarTitle(
        ChatInfo.Group(groupInfo),
        imageSize = 60.dp,
        iconColor = if (isInDarkTheme()) GroupDark else SettingsSecondaryLight
      )
    }
    SectionSpacer()

    if (contactsToAdd.isEmpty()) {
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          stringResource(R.string.no_contacts_to_add),
          Modifier.padding(),
          color = HighOrLowlight
        )
      }
    } else {
      SectionView {
        SectionItemView {
          RoleSelectionRow(groupInfo, selectedRole)
        }
        SectionDivider()
        InviteMembersButton(inviteMembers, disabled = selectedContacts.isEmpty())
      }
      SectionCustomFooter {
        InviteSectionFooter(selectedContactsCount = selectedContacts.count(), clearSelection)
      }
      SectionSpacer()

      SectionView {
        ContactList(contacts = contactsToAdd, selectedContacts, groupInfo, addContact, removeContact)
      }
      SectionSpacer()
    }
  }
}

@Composable
private fun RoleSelectionRow(groupInfo: GroupInfo, selectedRole: MutableState<GroupMemberRole>) {
  Row(
    Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    val values = GroupMemberRole.values().filter { it <= groupInfo.membership.memberRole }.map { it to it.text }
    ExposedDropDownSettingRow(
      generalGetString(R.string.new_member_role),
      values,
      selectedRole,
      icon = null,
      enabled = remember { mutableStateOf(true) },
      onSelected = { selectedRole.value = it }
    )
  }
}

@Composable
fun InviteMembersButton(onClick: () -> Unit, disabled: Boolean) {
  SettingsActionItem(
    Icons.Outlined.Check,
    stringResource(R.string.invite_to_group_button),
    click = onClick,
    textColor = MaterialTheme.colors.primary,
    iconColor = MaterialTheme.colors.primary,
    disabled = disabled,
  )
}

@Composable
fun InviteSectionFooter(selectedContactsCount: Int, clearSelection: () -> Unit) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (selectedContactsCount >= 1) {
      Text(
        String.format(generalGetString(R.string.num_contacts_selected), selectedContactsCount),
        color = HighOrLowlight,
        fontSize = 12.sp
      )
      Box(
        Modifier.clickable { clearSelection() }
      ) {
        Text(
          stringResource(R.string.clear_contacts_selection_button),
          color = MaterialTheme.colors.primary,
          fontSize = 12.sp
        )
      }
    } else {
      Text(
        stringResource(R.string.no_contacts_selected),
        color = HighOrLowlight,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
fun ContactList(
  contacts: List<Contact>,
  selectedContacts: SnapshotStateList<Long>,
  groupInfo: GroupInfo,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit
) {
  Column {
    contacts.forEachIndexed { index, contact ->
      ContactCheckRow(
        contact, groupInfo, addContact, removeContact,
        checked = selectedContacts.contains(contact.apiId)
      )
      if (index < contacts.lastIndex) {
        SectionDivider()
      }
    }
  }
}

@Composable
fun ContactCheckRow(
  contact: Contact,
  groupInfo: GroupInfo,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit,
  checked: Boolean
) {
  val prohibitedToInviteIncognito = !groupInfo.membership.memberIncognito && contact.contactConnIncognito
  val icon: ImageVector
  val iconColor: Color
  if (prohibitedToInviteIncognito) {
    icon = Icons.Filled.TheaterComedy
    iconColor = HighOrLowlight
  } else if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = MaterialTheme.colors.primary
  } else {
    icon = Icons.Outlined.Circle
    iconColor = HighOrLowlight
  }
  SectionItemView(click = {
    if (prohibitedToInviteIncognito) {
      showProhibitedToInviteIncognitoAlertDialog()
    } else if (!checked)
      addContact(contact.apiId)
    else
      removeContact(contact.apiId)
  }) {
    ProfileImage(size = 36.dp, contact.image)
    Spacer(Modifier.width(DEFAULT_SPACE_AFTER_ICON))
    Text(
      contact.chatViewName, maxLines = 1, overflow = TextOverflow.Ellipsis,
      color = if (prohibitedToInviteIncognito) HighOrLowlight else Color.Unspecified
    )
    Spacer(Modifier.fillMaxWidth().weight(1f))
    Icon(
      icon,
      contentDescription = stringResource(R.string.icon_descr_contact_checked),
      tint = iconColor
    )
  }
}

fun showProhibitedToInviteIncognitoAlertDialog() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.invite_prohibited),
    text = generalGetString(R.string.invite_prohibited_description),
    confirmText = generalGetString(R.string.ok),
  )
}

@Preview
@Composable
fun PreviewAddGroupMembersLayout() {
  SimpleXTheme {
    AddGroupMembersLayout(
      groupInfo = GroupInfo.sampleData,
      contactsToAdd = listOf(Contact.sampleData, Contact.sampleData, Contact.sampleData),
      selectedContacts = remember { mutableStateListOf() },
      selectedRole = remember { mutableStateOf(GroupMemberRole.Admin) },
      inviteMembers = {},
      clearSelection = {},
      addContact = {},
      removeContact = {}
    )
  }
}
