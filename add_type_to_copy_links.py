from lxml import etree
import sys
import os
import re

def add_type_to_copy_links(network_xml_path, output_xml_path):
    # Parse the network XML file
    try:
        tree = etree.parse(network_xml_path)
        root = tree.getroot()
        print(f"XML file '{network_xml_path}' successfully loaded.")
    except Exception as e:
        print(f"Error reading XML file: {e}")
        sys.exit(1)

    # Retrieve namespaces, if any
    nsmap = root.nsmap
    ns = {}
    if None in nsmap:
        ns['ns'] = nsmap[None]
    else:
        ns['ns'] = ''

    # Find the <links> section, considering namespaces
    if ns['ns']:
        links_section = root.find('.//ns:links', namespaces=ns)
    else:
        links_section = root.find('links')
    
    if links_section is None:
        print("Error: <links> section not found in the XML file.")
        sys.exit(1)
    else:
        print("<links> section found.")

    # Create a dictionary to store types of original links
    link_types = {}
    for link in links_section.findall('ns:link' if ns['ns'] else 'link', namespaces=ns):
        link_id = link.get('id')
        # Exclude copies and bike links
        if '_copy' in link_id or '_bike' in link_id:
            continue
        # Search for the 'type' attribute within <attributes>
        attributes = link.find('ns:attributes' if ns['ns'] else 'attributes', namespaces=ns)
        if attributes is not None:
            type_attr = attributes.find(".//ns:attribute[@name='type']" if ns['ns'] else ".//attribute[@name='type']", namespaces=ns)
            if type_attr is not None and type_attr.text:
                link_types[link_id] = type_attr.text
                print(f"Parent link '{link_id}' has type '{type_attr.text}'")
            else:
                print(f"Parent link '{link_id}' does not have a 'type' attribute")
        else:
            print(f"Parent link '{link_id}' does not have an <attributes> section")

    # Regular expression to extract parent ID
    # It captures everything before '_copy', ignoring any suffixes like '#2'
    copy_pattern = re.compile(r'^(.*?)_copy(?:#\d+)?$')

    # Process copy links
    num_copied = 0
    num_skipped = 0
    for link in links_section.findall('ns:link' if ns['ns'] else 'link', namespaces=ns):
        link_id = link.get('id')
        if '_copy' in link_id:
            print(f"Processing copy link '{link_id}'")
            match = copy_pattern.match(link_id)
            if match:
                parent_id = match.group(1)
                parent_type = link_types.get(parent_id)
                if parent_type:
                    # Find or create the <attributes> section
                    attributes = link.find('ns:attributes' if ns['ns'] else 'attributes', namespaces=ns)
                    if attributes is None:
                        attributes = etree.SubElement(link, 'attributes')
                        print(f"Created new <attributes> section for link '{link_id}'")
                    # Find existing 'type' attribute
                    type_attr = attributes.find(".//ns:attribute[@name='type']" if ns['ns'] else ".//attribute[@name='type']", namespaces=ns)
                    if type_attr is None:
                        # Create a new 'type' attribute
                        type_attr = etree.SubElement(attributes, 'attribute')
                        type_attr.set('name', 'type')
                        type_attr.set('class', 'java.lang.String')
                        type_attr.text = parent_type
                        print(f"Added type '{parent_type}' to link '{link_id}'")
                    else:
                        # Update the existing 'type' attribute
                        type_attr.text = parent_type
                        print(f"Updated type '{parent_type}' for link '{link_id}'")
                    num_copied += 1
                else:
                    print(f"Warning: Parent link for '{link_id}' not found or does not have a type. Type not assigned.")
                    num_skipped += 1
            else:
                print(f"Warning: Link '{link_id}' does not match the expected format. Type not assigned.")
                num_skipped += 1

    print(f"Processing completed. Types assigned to {num_copied} copy links. {num_skipped} copy links were skipped due to missing parent links or incorrect ID format.")

    # Save the modified XML file
    try:
        tree.write(output_xml_path, pretty_print=True, xml_declaration=True, encoding='UTF-8')
        print(f"Modified network file saved as '{output_xml_path}'")
    except Exception as e:
        print(f"Error saving XML file: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 add_type_to_copy_links.py <path_to_input_network_XML> <path_to_output_network_XML>")
        sys.exit(1)

    input_network_xml = sys.argv[1]
    output_network_xml = sys.argv[2]

    if not os.path.isfile(input_network_xml):
        print(f"Error: File '{input_network_xml}' does not exist.")
        sys.exit(1)

    add_type_to_copy_links(input_network_xml, output_network_xml)

