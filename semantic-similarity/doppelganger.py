import os
from dataclasses import dataclass
from ssl import create_default_context
from statistics import mean
from typing import Dict

from elasticsearch import Elasticsearch, helpers

es_index = "lowyat_old"


@dataclass
class Topic:
    id: str
    embedding = list()
    user_posts_embedding: list
    similar_topics = set()


@dataclass
class User:
    name: str
    doppelgangers = set()


@dataclass
class Doppelganger:
    scores: list
    name: str
    average_score = 0


def find_doppelganger(username):
    context = create_default_context(cafile=os.environ['SELFSIGNED_CERT'])

    es = Elasticsearch(
        hosts="https://dev.k8s.internal/elasticsearch",
        http_auth=("elastic", os.environ['ELASTIC_PASSWORD']),
        ssl_context=context,
        request_timeout=60
    )

    print("Elastic Search Local Cluster Status: {}".format(es.cluster.health()['status']))

    topics = get_topics_and_posts_user_posted_in(es, username)
    get_topic_embeddings(es, topics)
    get_similar_topics(es, topics)
    doppelgangers = get_similar_user_posts(es, topics)

    sorted_dg = sorted(list(doppelgangers.values()), reverse=True, key=lambda x: (len(x.scores), x.average_score))
    for i in sorted_dg:
        print("Username ", i.name, "Avg score ", i.average_score, "Scores ", i.scores)


def get_topics_and_posts_user_posted_in(es, username):
    topics: Dict[str, Topic] = dict()
    search_body = {
        "size": 100,
        "query": {
            "match": {
                "username": username
            }
        },
        "fields": [
            "type", "content", "post_embedding",
        ],
        "post_filter": {
            "term": {
                "type": "post"
            }
        },
        "_source": False

    }

    resp = helpers.scan(
        es,
        index=es_index,
        body=search_body,
        scroll='3m',  # time value for search
    )
    for num, doc in enumerate(resp):
        try:
            topic_id = doc["fields"]["type"][0]["parent"]
            post_embedding = doc["fields"]["post_embedding"]
            if topic_id not in topics.keys():
                topic = Topic(topic_id, user_posts_embedding=[post_embedding])
                topics[topic_id] = topic
            else:
                topics[topic_id].user_posts_embedding.append(post_embedding)

        except (KeyError, TypeError) as e:
            print(e)
            print("Missing parent information - embeddings may not have been generated for {}".format(doc["_id"]))

    return topics


def get_topic_embeddings(es, topics):
    for topic in topics.values():
        resp = es.get(index=es_index, id=topic.id, _source="topic_embedding")
        try:
            embedding = resp["_source"]["topic_embedding"]
            topics[topic.id].embedding = embedding
        except (KeyError, TypeError):
            print("Missing parent information - embeddings may not have been generated for {}".format(topic["_id"]))


def get_similar_topics(es, topics):
    for num, topic in enumerate(topics.values()):
        search_body = {
            "size": 10,
            "query": {
                "script_score": {
                    "query": {
                        "bool": {
                            "must_not": {
                                "terms": {
                                    "_id": [t.id for t in topics.values()]
                                }
                            },
                            "must": {
                                "match": {
                                    "type": "topic"
                                }
                            }
                        }
                    },
                    "script": {
                        "source": "cosineSimilarity(params.query_vector, 'topic_embedding') + 1.0",
                        "params": {
                            "query_vector": topic.embedding
                        }
                    },

                }
            },
            "fields": [
                "id"
            ],
            "_source": False

        }

        resp = es.search(
            index=es_index,
            body=search_body
        )

        try:
            hits = resp["hits"]["hits"]
            similar_topic_ids = set()
            for doc in hits:
                topic_id = doc["fields"]["id"]
                similar_topic_ids.add(topic_id[0])
            topics[topic.id].similar_topics = similar_topic_ids
        except (KeyError, TypeError) as e:
            print("Malformed search results.")


def get_similar_user_posts(es, topics):
    doppelgangers = dict()
    for topic in topics.values():
        for post_embedding in topic.user_posts_embedding:
            search_body = {
                "size": 10,
                "query": {
                    "script_score": {
                        "query": {
                            "bool": {
                                "must": {
                                    "has_parent": {
                                        "parent_type": "topic",
                                        "query": {
                                            "terms": {
                                                "id": list(topic.similar_topics)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        "script": {
                            "source": "cosineSimilarity(params.query_vector, 'post_embedding') + 1.0",
                            "params": {
                                "query_vector": post_embedding
                            }
                        },

                    }
                },
                "fields": [
                    "id", "username", "content"
                ],
                "_source": False
            }
            resp = es.search(
                index=es_index,
                body=search_body
            )

            try:
                hits = resp["hits"]["hits"]
                for doc in hits:
                    score = doc["_score"]
                    username = doc["fields"]["username"][0]
                    if username not in doppelgangers.keys():
                        doppelgangers[username] = Doppelganger([score], username)
                    else:
                        doppelgangers[username].scores.append(score)
                    doppelgangers[username].average_score = calc_avg_score(doppelgangers[username].scores)

            except (KeyError, TypeError) as e:
                print(e)
                print("Malformed search results.")

    return doppelgangers


def calc_avg_score(scores):
    return mean(scores)


if __name__ == '__main__':
    find_doppelganger("AdrianaMaisarah")
