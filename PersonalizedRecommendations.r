#movielens data preparation
#laod data
raw_data = read.csv("u.data",sep="\t",header=F)
colnames(raw_data) = c("UserId","MovieId","Rating","TimeStamp")
ratings = raw_data[,1:3]
movies = read.csv("u.item",sep="|",header=F)
colnames(movies) = c("MovieId","MovieTitle","ReleaseDate","VideoReleaseDate","IMDbURL","Unknown","Action","Adventure","Animation","Children","Comedy","Crime","Documentary","Drama","Fantasy","FilmNoir","Horror","Musical","Mystery","Romance","SciFi","Thriller","War","Western")
names(movies)
str(movies)

users = read.csv("u.user",sep="|",header=F)
colnames(users) = c("UserId","Age","Gender","Occupation","ZipCode")
users = users[,-c(4,5)]

#basic stats of data
dim(movies)
dim(users)


#merging user item rating with user profile
ratings = merge(x = ratings, y = movies, by = "MovieId", all.x = TRUE)

names(ratings)

#remove unwanted columns
#ratings = ratings[,-c(4,8:12)]

#conversion to categorical
nrat = unlist(lapply(ratings$Rating,function(x)
{
  if(x>3) {return(1)}
  else {return(0)}
}))
ratings = cbind(ratings,nrat)

apply(ratings[,-c(1:3,23)],2,function(x)table(x))

scaled_ratings = ratings[,-c(3,4)]

scaled_ratings=scale(scaled_ratings[,-c(1,2,21)])

scaled_ratings = cbind(scaled_ratings,ratings[,c(1,2,23)])

head(scaled_ratings)

# create a randomize index object of all the data. Then we use this
# indexes to divide the train and test sets
set.seed(7)
which_train <- sample(x = c(TRUE, FALSE), size = nrow(scaled_ratings),replace = TRUE, prob = c(0.8, 0.2))
model_data_train <- scaled_ratings[which_train, ]
model_data_test <- scaled_ratings[!which_train, ]

dim(model_data_train)
dim(model_data_test)

# converting the integer nrat variable to factor format
library(randomForest)
fit = randomForest(as.factor(nrat)~., data = model_data_train[,-c(19,20)])

fit

summary(fit)

predictions <- predict(fit, model_data_test[,-c(19,20,21)], type="class")

#building confusion matrix
cm = table(predictions,model_data_test$nrat)
(accuracy <- sum(diag(cm)) / sum(cm))
(precision <- diag(cm) / rowSums(cm))
recall <- (diag(cm) / colSums(cm))

recall

# generate the top-N recommendations to a user ID (943)

#1. Create a DataFrame containing all the movies not rated by the active user (943).

#extract distinct movieids
totalMovieIds = unique(movies$MovieId)

#a function to generate dataframe which creates non-rated
# movies by active user and set rating to 0;
nonratedmoviedf = function(userid){
  ratedmovies = raw_data[raw_data$UserId==userid,]$MovieId
  non_ratedmovies = totalMovieIds[!totalMovieIds %in%
                                    ratedmovies]
  df = data.frame(cbind(rep(userid),non_ratedmovies,0))
  names(df) = c("UserId","MovieId","Rating")
  return(df)
}
#extracting non-rated movies for active userid 943
activeusernonratedmoviedf = nonratedmoviedf(943)

# 2. Build a profile for this active user via DataFrame

activeuserratings = merge(x = activeusernonratedmoviedf,
                          y = movies, by = "MovieId", all.x = TRUE)

# 3. Predict ratings, sort and generate 10 recommendations.

predictions <- predict(fit, activeuserratings[,-c(1:4)],type="class")
#creating a dataframe from the results
recommend = data.frame(movieId = activeuserratings$MovieId,predictions)
# remove all the movies which the model has predicted as 0 and
# then use the remaining items as more probable movies
# which might be liked by the active user.
recommend = recommend[which(recommend$predictions == 1),]

dim(recommend)