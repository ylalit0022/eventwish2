#!/usr/bin/env python3
"""
Generate Festival Notification JSON for Firebase Remote Config

This script generates JSON for festival notifications that can be uploaded to Firebase Remote Config.
It creates entries for common festivals with proper dates for the specified year.
"""

import json
import argparse
from datetime import datetime

def generate_festival_json(year):
    """Generate festival JSON for the specified year."""
    
    # Define festival templates with approximate dates
    festivals = {
        "diwali": {
            "id": f"diwali_{year}",
            "title": f"Diwali Festival {year}",
            "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-10-01T00:00:00Z",  # Start notification 1 month before
            "enddate": f"{year}-11-12T23:59:59Z",    # Approximate Diwali date
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "18:30"
        },
        "christmas": {
            "id": f"christmas_{year}",
            "title": f"Christmas {year}",
            "body": "Christmas is almost here! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-11-25T00:00:00Z",  # Start notification 1 month before
            "enddate": f"{year}-12-25T23:59:59Z",    # Christmas date
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "09:00"
        },
        "newyear": {
            "id": f"newyear_{year+1}",
            "title": f"New Year {year+1}",
            "body": "Get ready for New Year celebrations! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-12-01T00:00:00Z",  # Start notification 1 month before
            "enddate": f"{year+1}-01-01T23:59:59Z",  # New Year date
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "20:00"
        },
        "holi": {
            "id": f"holi_{year}",
            "title": f"Holi Festival {year}",
            "body": "Holi celebration is coming soon! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-02-15T00:00:00Z",  # Start notification ~1 month before
            "enddate": f"{year}-03-25T23:59:59Z",    # Approximate Holi date (varies)
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "10:00"
        },
        "eid": {
            "id": f"eid_{year}",
            "title": f"Eid Festival {year}",
            "body": "Eid celebration is coming soon! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-04-01T00:00:00Z",  # Start notification ~1 month before
            "enddate": f"{year}-05-15T23:59:59Z",    # Approximate Eid date (varies)
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "19:00"
        },
        "thanksgiving": {
            "id": f"thanksgiving_{year}",
            "title": f"Thanksgiving {year}",
            "body": "Thanksgiving is almost here! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-11-01T00:00:00Z",  # Start notification ~3 weeks before
            "enddate": f"{year}-11-30T23:59:59Z",    # Approximate Thanksgiving date (4th Thursday in November)
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "08:00"
        },
        "halloween": {
            "id": f"halloween_{year}",
            "title": f"Halloween {year}",
            "body": "Halloween is coming! Only {{countdown}} left!",
            "status": True,
            "startdate": f"{year}-10-01T00:00:00Z",  # Start notification 1 month before
            "enddate": f"{year}-10-31T23:59:59Z",    # Halloween date
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "17:00"
        }
    }
    
    # Create the output formats
    
    # 1. Array of all festivals for the "events" format
    events_array = list(festivals.values())
    
    # 2. Firebase Remote Config format with both single and multiple events
    firebase_format = {
        "event_push": {
            "id": "single_event",
            "title": "Festival Notifications",
            "body": "Don't miss upcoming festivals! Check the app for details.",
            "status": True,
            "startdate": f"{year}-01-01T00:00:00Z",
            "enddate": f"{year}-12-31T23:59:59Z",
            "deeplinksupport": "eventwish://open/festival_notification",
            "showTime": "08:00"
        },
        "events": events_array
    }
    
    return {
        "events_array": events_array,
        "firebase_format": firebase_format,
        "individual_festivals": festivals
    }

def main():
    parser = argparse.ArgumentParser(description='Generate festival notification JSON for Firebase Remote Config')
    parser.add_argument('--year', type=int, default=datetime.now().year,
                        help='Year to generate festivals for (default: current year)')
    parser.add_argument('--output', type=str, default='festivals.json',
                        help='Output file name (default: festivals.json)')
    parser.add_argument('--format', type=str, choices=['array', 'firebase', 'individual', 'all'], 
                        default='all', help='Output format (default: all)')
    
    args = parser.parse_args()
    
    print(f"Generating festival notifications for {args.year}...")
    
    result = generate_festival_json(args.year)
    
    # Determine which format to output
    if args.format == 'array':
        output_json = result['events_array']
    elif args.format == 'firebase':
        output_json = result['firebase_format']
    elif args.format == 'individual':
        output_json = result['individual_festivals']
    else:  # 'all'
        output_json = result
    
    # Write to file
    with open(args.output, 'w') as f:
        json.dump(output_json, f, indent=2)
    
    print(f"Generated {len(result['events_array'])} festival notifications")
    print(f"Output written to {args.output}")

if __name__ == "__main__":
    main() 