import os
from ssl import create_default_context

from elasticsearch import Elasticsearch, helpers
from sentence_transformers import SentenceTransformer


def post_dense_vector(es, model):
    # Cant use eland due to scroll timeout issue
    # lowyat = ed.DataFrame(es, es_index_pattern="lowyat_old")
    search_body = {
        "size": 100,
        "query": {
            "match_all": {}
        }
    }

    resp = helpers.scan(
        es,
        index="lowyat_old",
        body=search_body,
        scroll='3m',  # time value for search
    )

    for num, doc in enumerate(resp):
        content = doc["_source"]['content']
        print('\n', num, "", doc["_id"])

        if content and 'post_embedding' not in doc['_source']:
            print('\n', num, '', content)
            emb = model.encode(content)
            es.update(index="lowyat_old", id=doc["_source"]["id"], body={"doc": {"post_embedding": emb}})


def update_embedding():
    context = create_default_context(cafile= os.environ['SELFSIGNED_CERT'])

    es = Elasticsearch(
        hosts="https://dev.k8s.internal/elasticsearch",
        http_auth=("elastic", os.environ['ELASTIC_PASSWORD']),
        ssl_context=context,
        request_timeout=60
    )

    print("Elastic Search Local Cluster Status: {}".format(es.cluster.health()['status']))

    model = SentenceTransformer('stsb-xlm-r-multilingual')
    topic_dense_vector(es, model)
    post_dense_vector(es, model)


def topic_dense_vector(es, model):
    search_body = {
        # small size specified since k8s ingress doesnt like large payloads
        # may be related to https://github.com/kubernetes/kubernetes/issues/74839
        # or specify fields to return instead of entire _source
        "size": 50,
        "query": {
            "term": {"type": "topic"}
        }
    }

    resp = helpers.scan(
        es,
        index="lowyat_old",
        body=search_body,
        scroll='5m',  # time value for search
    )

    for num, doc in enumerate(resp):
        title = doc["_source"]['title']
        print('\n', num, "", doc["_id"])

        if title and 'topic_embedding' not in doc['_source']:
            print('\n', num, '', title)
            emb = model.encode(title)
            es.update(index="lowyat_old", id=doc["_source"]["id"], body={"doc": {"topic_embedding": emb}})


if __name__ == '__main__':
    update_embedding()
