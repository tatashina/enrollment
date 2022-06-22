# encoding=utf8

import json
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

API_BASEURL = "http://localhost:80"

ROOT_ID = "1"
OFFER_ID_WITHOUT_PARENT = 10
CATEGORY_ID_WITHOUT_PARENT = 11

IMPORT_BATCHES = [
    {
        "items": [
            {
                "id": "1",
                "name": "1name",
                "parentId": None,
                "type": "CATEGORY"
            },
            {
                "id": "2",
                "name": "2name",
                "parentId": "1",
                "type": "CATEGORY"
            },
            {
                "id": "3",
                "name": "3name",
                "parentId": "2",
                "price": 300,
                "type": "OFFER"
            },
            {
                "id": "4",
                "name": "4name",
                "parentId": "2",
                "price": 400,
                "type": "OFFER"
            },
            {
                "id": "5",
                "name": "5name",
                "parentId": "1",
                "type": "CATEGORY"
            },
            {
                "id": "6",
                "name": "6name",
                "parentId": "5",
                "type": "CATEGORY"
            },
            {
                "id": "7",
                "name": "7",
                "parentId": "6",
                "price": 777,
                "type": "OFFER"
            },
            {
                "id": "10",
                "name": "10",
                "parentId": None,
                "price": 1000,
                "type": "OFFER"
            },
            {
                "id": "11",
                "name": "11name",
                "parentId": None,
                "type": "CATEGORY"
            }
        ],
        "updateDate": "2022-01-01T12:34:56.789Z"
    },
    {
        "items": [
            {
                "id": "4",
                "name": "4name",
                "parentId": "5",
                "price": 400,
                "type": "OFFER"
            },
            {
                "id": "11",
                "name": "11nameUpdated",
                "parentId": None,
                "type": "CATEGORY"
            }
        ],
        "updateDate": "2022-02-02T22:12:34.567Z"
    },
    {
        "items": [
            {
                "id": "8",
                "name": "8name",
                "parentId": "2",
                "price": 800,
                "type": "OFFER"
            }
        ],
        "updateDate": "2022-03-03T13:33:00.000Z"
    }
]

EXPECTED_OFFER = {
    "id": "10",
    "name": "10",
    "parentId": None,
    "price": 1000,
    "type": "OFFER",
    "date": "2022-01-01T12:34:56.789Z",
    "children": None
}

EXPECTED_CATEGORY = {
    "id": "11",
    "name": "11nameUpdated",
    "parentId": None,
    "type": "CATEGORY",
    "price": None,
    "date": "2022-02-02T22:12:34.567Z",
    "children": []
}

EXPECTED_TREE = {
    "id": "1",
    "name": "1name",
    "parentId": None,
    "type": "CATEGORY",
    "price": 569,
    "date": "2022-03-03T13:33:00.000Z",
    "children": [
        {
            "id": "2",
            "name": "2name",
            "parentId": "1",
            "type": "CATEGORY",
            "date": "2022-03-03T13:33:00.000Z",
            "price": 550,
            "children": [
                {
                    "id": "3",
                    "name": "3name",
                    "parentId": "2",
                    "price": 300,
                    "type": "OFFER",
                    "date": "2022-01-01T12:34:56.789Z",
                    "children": None
                },
                {
                    "id": "8",
                    "name": "8name",
                    "parentId": "2",
                    "price": 800,
                    "type": "OFFER",
                    "date": "2022-03-03T13:33:00.000Z",
                    "children": None
                }
            ]
        },
        {
            "id": "5",
            "name": "5name",
            "parentId": "1",
            "type": "CATEGORY",
            "price": 588,
            "date": "2022-02-02T22:12:34.567Z",
            "children": [
                {
                    "id": "4",
                    "name": "4name",
                    "parentId": "5",
                    "price": 400,
                    "type": "OFFER",
                    "date": "2022-02-02T22:12:34.567Z",
                    "children": None
                },
                {
                    "id": "6",
                    "name": "6name",
                    "parentId": "5",
                    "price": 777,
                    "type": "CATEGORY",
                    "date": "2022-01-01T12:34:56.789Z",
                    "children": [
                        {
                            "id": "7",
                            "name": "7",
                            "parentId": "6",
                            "price": 777,
                            "type": "OFFER",
                            "date": "2022-01-01T12:34:56.789Z",
                            "children": None
                        }
                    ]
                }
            ]
        }
    ]
}
EXPECTED_SALES = [
    {
        "date": "2022-02-02T22:12:34.567Z",
        "id": "4",
        "name": "4name",
        "parentId": "5",
        "price": 400,
        "type": "OFFER"
    },
    {
        "date": "2022-03-03T13:33:00.000Z",
        "id": "8",
        "name": "8name",
        "parentId": "2",
        "price": 800,
        "type": "OFFER"
    }
]


RESPONSE_EMPTY = {
    "items": []
}



def request(path, method="GET", data=None, json_response=False):
    try:
        params = {
            "url": f"{API_BASEURL}{path}",
            "method": method,
            "headers": {},
        }

        if data:
            params["data"] = json.dumps(
                data, ensure_ascii=False).encode("utf-8")
            params["headers"]["Content-Length"] = len(params["data"])
            params["headers"]["Content-Type"] = "application/json"

        req = urllib.request.Request(**params)

        with urllib.request.urlopen(req) as res:
            res_data = res.read().decode("utf-8")
            if json_response:
                res_data = json.loads(res_data)
            return (res.getcode(), res_data)
    except urllib.error.HTTPError as e:
        return (e.getcode(), None)


def deep_sort_children(node):
    if node.get("children"):
        node["children"].sort(key=lambda x: x["id"])

        for child in node["children"]:
            deep_sort_children(child)


def print_diff(expected, response):
    with open("expected.json", "w") as f:
        json.dump(expected, f, indent=2, ensure_ascii=False, sort_keys=True)
        f.write("\n")

    with open("response.json", "w") as f:
        json.dump(response, f, indent=2, ensure_ascii=False, sort_keys=True)
        f.write("\n")

    subprocess.run(["git", "--no-pager", "diff", "--no-index",
                    "expected.json", "response.json"])


def test_import():
    for index, batch in enumerate(IMPORT_BATCHES):
        print(f"Importing batch {index}")
        status, _ = request("/imports", method="POST", data=batch)

        assert status == 200, f"Expected HTTP status code 200, got {status}"

    print("Test import passed.")


def test_nodes():
    status, response = request(f"/nodes/{ROOT_ID}", json_response=True)
    # print(json.dumps(response, indent=2, ensure_ascii=False))

    assert status == 200, f"Expected HTTP status code 200, got {status}"

    deep_sort_children(response)
    deep_sort_children(EXPECTED_TREE)
    if response != EXPECTED_TREE:
        print_diff(EXPECTED_TREE, response)
        print("Response tree doesn't match expected tree.")
        sys.exit(1)

    print("Test nodes passed.")

def test_offer_without_parent():
    status, response = request(f"/nodes/{OFFER_ID_WITHOUT_PARENT}", json_response=True)
    assert status == 200, f"Expected HTTP status code 200, got {status}"

    if response != EXPECTED_OFFER:
        print_diff(EXPECTED_OFFER, response)
        print("Response doesn't match expected offer.")
        sys.exit(1)

    print("Test offer without parent passed.")

def test_category_without_parent():
    status, response = request(f"/nodes/{CATEGORY_ID_WITHOUT_PARENT}", json_response=True)
    assert status == 200, f"Expected HTTP status code 200, got {status}"

    if response != EXPECTED_CATEGORY:
        print_diff(EXPECTED_CATEGORY, response)
        print("Response doesn't match expected category.")
        sys.exit(1)

    print("Test category without parent passed.")

def test_sales():
    params = urllib.parse.urlencode({
        "date": "2022-02-03T22:12:34.567Z"
    })
    status, response = request(f"/sales?{params}", json_response=True)
    assert status == 200, f"Expected HTTP status code 200, got {status}"
    response = sorted(response["items"], key=lambda x: x["id"])
    if response != EXPECTED_SALES:
        print_diff(EXPECTED_SALES, response)
        print("Response sales doesn't match expected sales.")
        sys.exit(1)

    print("Test sales passed.")

def test_sales_empty():

    params = urllib.parse.urlencode({
        "date": "2023-01-01T12:34:56.789Z"
    })
    status, response = request(f"/sales?{params}", json_response=True)
    assert status == 200, f"Expected HTTP status code 200, got {status}"

    if response != RESPONSE_EMPTY:
        print_diff(RESPONSE_EMPTY, response)
        print("Response sales doesn't match expected empty.")
        sys.exit(1)

    print("Test sales empty passed.")

def test_delete():
    status, _ = request(f"/delete/{ROOT_ID}", method="DELETE")
    assert status == 200, f"Expected HTTP status code 200, got {status}"

    status, _ = request(f"/nodes/{ROOT_ID}", json_response=True)
    assert status == 404, f"Expected HTTP status code 404, got {status}"

    status, _ = request(f"/delete/{OFFER_ID_WITHOUT_PARENT}", method="DELETE")
    assert status == 200, f"Expected HTTP status code 200, got {status}"

    status, _ = request(f"/nodes/{OFFER_ID_WITHOUT_PARENT}", json_response=True)
    assert status == 404, f"Expected HTTP status code 404, got {status}"

    status, _ = request(f"/delete/{CATEGORY_ID_WITHOUT_PARENT}", method="DELETE")
    assert status == 200, f"Expected HTTP status code 200, got {status}"

    status, _ = request(f"/nodes/{CATEGORY_ID_WITHOUT_PARENT}", json_response=True)
    assert status == 404, f"Expected HTTP status code 404, got {status}"

    print("Test delete passed.")


def test_all():
    test_import()
    test_nodes()
    test_offer_without_parent()
    test_category_without_parent()

    test_sales()
    test_sales_empty()

    test_delete()

def main():
    global API_BASEURL
    test_name = None

    for arg in sys.argv[1:]:
        if re.match(r"^https?://", arg):
            API_BASEURL = arg
        elif test_name is None:
            test_name = arg

    if API_BASEURL.endswith('/'):
        API_BASEURL = API_BASEURL[:-1]

    if test_name is None:
        test_all()
    else:
        test_func = globals().get(f"test_{test_name}")
        if not test_func:
            print(f"Unknown test: {test_name}")
            sys.exit(1)
        test_func()


if __name__ == "__main__":
    main()
