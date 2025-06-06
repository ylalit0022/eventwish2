# Firebase Remote Config - Share Message Templates

## Overview
These templates can be uploaded to Firebase Remote Config under the parameter key `share_message`. They support placeholders that will be dynamically replaced by the app.

## Available Placeholders
- `{{sender_name}}` - Will be replaced with the sender's name
- `{{recipient_name}}` - Will be replaced with the recipient's name (if available)
- `{{share_url}}` - Will be replaced with the actual share URL

## General Template
```
Hey! {{sender_name}} created a special wish for you using EventWish. Check it out here: {{share_url}}
```

## Platform-Specific Templates

### WhatsApp
```
Hey! {{sender_name}} created a special wish for you using EventWish. Check it out here: {{share_url}}
```

### Facebook
```
{{sender_name}} just created a special wish for {{recipient_name}} with EventWish! Check it out here: {{share_url}}
```

### Email
```
Hello {{recipient_name}},

{{sender_name}} created a special wish for you using EventWish.

Check it out here: {{share_url}}

Enjoy!
```

### SMS
```
From {{sender_name}}: I created a special wish for you with EventWish! View it here: {{share_url}}
```

### Twitter/X
```
I just created a special wish with EventWish! Check it out: {{share_url}} #EventWish #SpecialWishes
```

### Instagram
```
I created something special with EventWish! Link in bio or DM for the link: {{share_url}} #EventWish #SpecialWishes
```

## JSON Format for Firebase Remote Config
```json
{
  "share_message": "Hey! {{sender_name}} created a special wish for you using EventWish. Check it out here: {{share_url}}",
  "share_message_whatsapp": "Hey! {{sender_name}} created a special wish for you using EventWish. Check it out here: {{share_url}}",
  "share_message_facebook": "{{sender_name}} just created a special wish for {{recipient_name}} with EventWish! Check it out here: {{share_url}}",
  "share_message_email": "Hello {{recipient_name}},\n\n{{sender_name}} created a special wish for you using EventWish.\n\nCheck it out here: {{share_url}}\n\nEnjoy!",
  "share_message_sms": "From {{sender_name}}: I created a special wish for you with EventWish! View it here: {{share_url}}",
  "share_message_twitter": "I just created a special wish with EventWish! Check it out: {{share_url}} #EventWish #SpecialWishes",
  "share_message_instagram": "I created something special with EventWish! Link in bio or DM for the link: {{share_url}} #EventWish #SpecialWishes"
}
```

## Implementation Notes
1. Upload the JSON format to Firebase Remote Config
2. Set default values for fallback
3. Use conditional targeting if needed for different user segments
4. Test message rendering with various placeholder combinations
5. Consider character limits for different platforms 